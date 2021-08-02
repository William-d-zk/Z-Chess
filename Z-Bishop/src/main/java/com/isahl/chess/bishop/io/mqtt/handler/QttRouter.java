/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.bishop.io.mqtt.handler;

import com.isahl.chess.bishop.io.mqtt.MqttProtocol;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IQoS;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * @date 2019-07-07
 */
public class QttRouter
        implements IQttRouter
{
    private final Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getName());

    private static class Subscribe
    {
        MqttProtocol mRetained;
        final Map<Long, IQoS.Level> _SessionMap = new ConcurrentSkipListMap<>();
        final Pattern               _Pattern;

        Subscribe(Pattern pattern) {_Pattern = pattern;}

        void remove(long session)
        {
            _SessionMap.remove(session);
        }
    }

    private final Map<Pattern, Subscribe>        _Topic2SessionsMap = new TreeMap<>(Comparator.comparing(Pattern::pattern));
    private final AtomicLong                     _AtomicIdentity    = new AtomicLong(Long.MIN_VALUE);
    private final Map<Long, Map<Long, IControl>> _QttIdentifierMap  = new ConcurrentSkipListMap<>();
    private final Queue<Pair<Long, Instant>>     _SessionIdleQueue  = new ConcurrentLinkedQueue<>();

    @Override
    public void retain(String topic, MqttProtocol msg)
    {
        Pattern pattern = topicToRegex(topic);
        _Topic2SessionsMap.computeIfAbsent(pattern, Subscribe::new).mRetained = msg;
    }

    @Override
    public Map<Long, IQoS.Level> broker(final String topic)
    {
        _Logger.debug("broker topic: %s", topic);
        return _Topic2SessionsMap.entrySet()
                                 .parallelStream()
                                 .map(entry->{
                                     Pattern pattern = entry.getKey();
                                     Subscribe subscribe = entry.getValue();
                                     return pattern.matcher(topic)
                                                   .matches() && !subscribe._SessionMap.isEmpty()
                                            ? subscribe._SessionMap : null;

                                 })
                                 .filter(Objects::nonNull)
                                 .map(Map::entrySet)
                                 .flatMap(Set::stream)
                                 .collect(Collectors.toMap(Map.Entry::getKey,
                                                           Map.Entry::getValue,
                                                           (l, r)->l.getValue() > r.getValue() ? l : r,
                                                           ConcurrentSkipListMap::new));
    }

    public Map<String, IQoS.Level> groupBy(long session)
    {
        _Logger.debug("group by :%#x", session);
        return _Topic2SessionsMap.entrySet()
                                 .parallelStream()
                                 .flatMap(entry->{
                                     Pattern pattern = entry.getKey();
                                     Subscribe subscribe = entry.getValue();
                                     Map<Long, IQoS.Level> sessionsLv = subscribe._SessionMap;
                                     return sessionsLv.entrySet()
                                                      .stream()
                                                      .filter(e->e.getKey() == session)
                                                      .map(e->new Pair<>(pattern.pattern(), e.getValue()));
                                 })
                                 .collect(Collectors.toMap(IPair::getFirst, IPair::getSecond));
    }

    @Override
    public IQoS.Level subscribe(String topic, IQoS.Level level, long session)
    {
        try {
            Pattern pattern = topicToRegex(topic);
            _Logger.debug("topic %s,pattern %s", topic, pattern);
            Subscribe subscribe = _Topic2SessionsMap.computeIfAbsent(pattern, p->new Subscribe(pattern));
            IQoS.Level lv = subscribe._SessionMap.computeIfPresent(session,
                                                                   (key, old)->old.getValue() > level.getValue() ? old
                                                                                                                 : level);
            if(lv == null) {
                subscribe._SessionMap.put(session, level);
                return level;
            }
            return lv;
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
            return IQoS.Level.FAILURE;
        }
    }

    private Pattern topicToRegex(String topic)
    {

        topic = topic.replaceAll("\\++", "+");
        topic = topic.replaceAll("#+", "#");
        topic = topic.replaceAll("(/#)+", "/#");

        if(Pattern.compile("#\\+|\\+#")
                  .asPredicate()
                  .test(topic))
        {
            throw new IllegalArgumentException("topic error " + topic);
        }
        if(!Pattern.compile("(/\\+)$")
                   .asPredicate()
                   .test(topic))
        {
            topic = topic.replaceAll("(\\+)$", "");
        }
        topic = topic.replaceAll("^\\+", "([^\\$/]+)");
        topic = topic.replaceAll("(/\\+)$", "(/?[^/]*)");
        topic = topic.replaceAll("/\\+", "/([^/]+)");
        topic = topic.replaceAll("^#", "([^\\$]*)");
        topic = topic.replaceAll("^/#", "(/.*)");
        topic = topic.replaceAll("/#", "(/?.*)");
        return Pattern.compile(topic);
    }

    @Override
    public void unsubscribe(String topic, long session)
    {
        _Topic2SessionsMap.forEach((pattern, subscribe)->{
            Matcher matcher = pattern.matcher(topic);
            if(matcher.matches()) {
                subscribe.remove(session);
            }
        });
    }

    @Override
    public long nextId()
    {
        return 0xFFFF & _AtomicIdentity.getAndIncrement();
    }

    @Override
    public void register(ICommand stateMessage, long session)
    {
        long msgId = stateMessage.getMsgId();
        if(_QttIdentifierMap.computeIfPresent(session, (key, _LocalIdMessageMap)->{
            IControl old = _LocalIdMessageMap.put(msgId, stateMessage);
            if(old == null) {
                _Logger.debug("retry recv: %s", stateMessage);
            }
            return _LocalIdMessageMap;
        }) == null)
        {
            //previous == null
            final Map<Long, IControl> _LocalIdMessageMap = new HashMap<>(7);
            _LocalIdMessageMap.put(msgId, stateMessage);
            _QttIdentifierMap.put(session, _LocalIdMessageMap);
            _Logger.debug("first recv: %s", stateMessage);
        }
    }

    @Override
    public boolean ack(ICommand stateMessage, long session)
    {
        int idleMax = stateMessage.session()
                                  .getReadTimeOutSeconds();
        long msgId = stateMessage.getMsgId();
        boolean[] acked = { true, true };
        if(acked[0] = _QttIdentifierMap.computeIfPresent(session, (key, old)->{
            _Logger.debug("ack %d @ %#x", msgId, session);
            acked[1] = old.remove(msgId) != null;
            return old.isEmpty() ? old : null;
        }) != null)
        {
            _Logger.debug("idle: %#x", session);
            _SessionIdleQueue.offer(new Pair<>(session, Instant.now()));
        }
        for(Iterator<Pair<Long, Instant>> it = _SessionIdleQueue.iterator(); it.hasNext(); ) {
            IPair pair = it.next();
            long rmSessionIndex = pair.getFirst();
            Instant idle = pair.getSecond();
            if(Instant.now()
                      .isAfter(idle.plusSeconds(idleMax)))
            {
                _QttIdentifierMap.remove(rmSessionIndex);
                it.remove();
            }
        }
        return acked[0] & acked[1];
    }

    @Override
    public void clean(long session)
    {
        Optional.ofNullable(_QttIdentifierMap.remove(session))
                .ifPresent(Map::clear);
        _Topic2SessionsMap.values()
                          .forEach(map->map.remove(session));
    }
}
