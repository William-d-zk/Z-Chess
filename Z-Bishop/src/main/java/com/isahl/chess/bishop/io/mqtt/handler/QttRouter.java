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
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IQoS;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * 
 * @date 2019-07-07
 */
public class QttRouter
        implements
        IQttRouter
{
    private final Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getName());

    private static class Subscribe
    {
        MqttProtocol          retained;
        Map<Long,
            IQoS.Level>       sessionMap;

        Subscribe(Map<Long,
                      IQoS.Level> map)
        {
            sessionMap = map;
        }

        void remove(long session)
        {
            sessionMap.remove(session);
            /*
            此处持续持有 map，避免多次new map的开销，如遇到内存大量管理成本放开如下
            
            if (sessionMap.isEmpty()) {
                sessionMap = null;
            }
            */
        }
    }

    private final Map<Pattern,
                      Subscribe>                 _Topic2SessionsMap = new TreeMap<>(Comparator.comparing(Pattern::pattern));
    private final AtomicLong                     _AtomicIdentity    = new AtomicLong(Long.MIN_VALUE);
    private final Map<Long,
                      Map<Long,
                          IControl>>             _QttIdentifierMap  = new ConcurrentSkipListMap<>();
    private final Queue<IPair>                   _SessionIdleQueue  = new ConcurrentLinkedQueue<>();

    @Override
    public void retain(String topic, MqttProtocol msg)
    {
        Pattern pattern = topicToRegex(topic);
        _Topic2SessionsMap.computeIfAbsent(pattern, p -> new Subscribe(new ConcurrentSkipListMap<>())).retained = msg;
    }

    @Override
    public Map<Long,
               IQoS.Level> broker(final String topic)
    {
        _Logger.debug("broker topic: %s", topic);
        return _Topic2SessionsMap.entrySet()
                                 .parallelStream()
                                 .map(entry ->
                                 {
                                     Pattern pattern = entry.getKey();
                                     Subscribe subcribe = entry.getValue();
                                     return pattern.matcher(topic)
                                                   .matches()
                                            && !subcribe.sessionMap.isEmpty() ? subcribe.sessionMap: null;

                                 })
                                 .filter(Objects::nonNull)
                                 .map(Map::entrySet)
                                 .flatMap(Set::stream)
                                 .collect(Collectors.toMap(Map.Entry::getKey,
                                                           Map.Entry::getValue,
                                                           (l, r) -> l.getValue() > r.getValue() ? l: r,
                                                           ConcurrentSkipListMap::new));
    }

    public Map<String,
               IQoS.Level> groupBy(long session)
    {
        _Logger.debug("group by :%#x", session);
        return _Topic2SessionsMap.entrySet()
                                 .parallelStream()
                                 .flatMap(entry ->
                                 {
                                     Pattern pattern = entry.getKey();
                                     Subscribe subscribe = entry.getValue();
                                     Map<Long,
                                         IQoS.Level> sessionsLv = subscribe.sessionMap;
                                     return sessionsLv.entrySet()
                                                      .stream()
                                                      .map(e -> new Triple<>(e.getKey(),
                                                                             pattern.pattern(),
                                                                             e.getValue()))
                                                      .filter(t -> t.getFirst() == session);
                                 })
                                 .collect(Collectors.toMap(Triple::getSecond, Triple::getThird));
    }

    @Override
    public IQoS.Level subscribe(String topic, IQoS.Level level, long session)
    {
        try {
            Pattern pattern = topicToRegex(topic);
            _Logger.debug("topic %s,pattern %s", topic, pattern);
            Subscribe subscribe = _Topic2SessionsMap.computeIfAbsent(pattern,
                                                                     p -> new Subscribe(new ConcurrentSkipListMap<>()));
            return subscribe.sessionMap.computeIfPresent(session,
                                                         (key, old) -> old.getValue() > level.getValue() ? old: level);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            return IQoS.Level.FAILURE;
        }
    }

    private Pattern topicToRegex(String topic)
    {

        topic = topic.replaceAll("\\++", "+");
        topic = topic.replaceAll("#+", "#");
        topic = topic.replaceAll("(/#)+", "/#");

        if (Pattern.compile("#\\+|\\+#")
                   .asPredicate()
                   .test(topic))
        {
            throw new IllegalArgumentException("topic error " + topic);
        }
        if (!Pattern.compile("(/\\+)$")
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
        _Topic2SessionsMap.forEach((pattern, subscribe) ->
        {
            Matcher matcher = pattern.matcher(topic);
            if (matcher.matches()) {
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
    public void register(ICommand stateMessage, long sessionIndex)
    {
        long msgId = stateMessage.getMsgId();
        if (_QttIdentifierMap.computeIfPresent(sessionIndex, (key, _LocalIdMessageMap) ->
        {
            IControl old = _LocalIdMessageMap.put(msgId, stateMessage);
            if (old == null) {
                _Logger.debug("retry recv: %s", stateMessage);
            }
            return _LocalIdMessageMap;
        }) == null) {
            final Map<Long,
                      IControl> _LocalIdMessageMap = new HashMap<>(7);
            _LocalIdMessageMap.put(msgId, stateMessage);
            _QttIdentifierMap.put(sessionIndex, _LocalIdMessageMap);
            _Logger.debug("first recv: %s", stateMessage);
        }
    }

    @Override
    public boolean ack(ICommand stateMessage, long session)
    {
        int idleMax = stateMessage.getSession()
                                  .getReadTimeOutSeconds();
        long msgId = stateMessage.getMsgId();
        boolean[] acked = {true,
                           true};
        if (acked[0] = _QttIdentifierMap.computeIfPresent(session, (key, old) ->
        {
            acked[1] = old.remove(msgId) == null;
            return old.isEmpty() ? old: null;
        }) != null) {
            _SessionIdleQueue.offer(new Pair<>(session, Instant.now()));
        }
        for (Iterator<IPair> it = _SessionIdleQueue.iterator(); it.hasNext();) {
            IPair pair = it.next();
            long rmSessionIndex = pair.getFirst();
            Instant idle = pair.getSecond();
            if (Instant.now()
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
                          .forEach(map -> map.remove(session));
    }
}
