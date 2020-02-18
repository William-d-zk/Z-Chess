/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2019 Z-Chess                                                
 *                                                                                
 * Permission is hereby granted, free of charge, to any person obtaining a copy   
 * of this software and associated documentation files (the "Software"), to deal  
 * in the Software without restriction, including without limitation the rights   
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell      
 * copies of the Software, and to permit persons to whom the Software is          
 * furnished to do so, subject to the following conditions:                       
 *                                                                                
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.                                
 *                                                                                
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR     
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,       
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE    
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER         
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE  
 * SOFTWARE.                                                                      
 */

package com.tgx.chess.bishop.io.mqtt.handler;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 * @date 2019-07-07
 */
public class QttRouter
        implements
        IQttRouter
{
    private final Logger                                   _Logger            = Logger.getLogger(getClass().getName());
    private final Map<Pattern,
                      Map<Long,
                          IQoS.Level>>                     _Topic2SessionsMap = new TreeMap<>(Comparator.comparing(Pattern::pattern));
    private final AtomicLong                               _AtomicIdentity    = new AtomicLong(Long.MIN_VALUE);
    private final Map<Long,
                      Map<Long,
                          IControl<ZContext>>>             _QttStatusMap      = new ConcurrentSkipListMap<>();
    private final Queue<IPair>                             _SessionIdleQueue  = new ConcurrentLinkedQueue<>();

    public Map<Long,
               IQoS.Level> broker(final String topic)
    {
        _Logger.info("broker topic: %s", topic);
        return _Topic2SessionsMap.entrySet()
                                 .parallelStream()
                                 .map(entry ->
                                 {
                                     Pattern pattern = entry.getKey();
                                     return pattern.matcher(topic)
                                                   .matches() ? entry.getValue()
                                                              : null;
                                 })
                                 .filter(Objects::nonNull)
                                 .map(Map::entrySet)
                                 .flatMap(Set::stream)
                                 .collect(Collectors.toMap(Map.Entry::getKey,
                                                           Map.Entry::getValue,
                                                           (l, r) -> l.getValue() > r.getValue() ? l
                                                                                                 : r,
                                                           ConcurrentSkipListMap::new));
    }

    public boolean addTopic(Pair<String,
                                 IQoS.Level> pair,
                            long sessionIndex)
    {
        String topic = pair.first();
        IQoS.Level qosLevel = pair.second();
        try {
            Pattern pattern = topicToRegex(topic);
            _Logger.info("topic %s,pattern %s", topic, pattern);
            Map<Long,
                IQoS.Level> value = _Topic2SessionsMap.get(pattern);
            if (Objects.isNull(value)) {
                value = new ConcurrentSkipListMap<>();
                _Topic2SessionsMap.put(pattern, value);
            }
            if (value.computeIfPresent(sessionIndex,
                                       (key, old) -> old.getValue() > qosLevel.getValue() ? old
                                                                                          : qosLevel) == null)
            {
                value.put(sessionIndex, qosLevel);
            }
            return true;
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
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
    public void removeTopic(String topic, long sessionIndex)
    {
        for (Iterator<Map.Entry<Pattern,
                                Map<Long,
                                    IQoS.Level>>> iterator = _Topic2SessionsMap.entrySet()
                                                                               .iterator(); iterator.hasNext();)
        {
            Map.Entry<Pattern,
                      Map<Long,
                          IQoS.Level>> entry = iterator.next();
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(topic);
            if (matcher.matches()) {
                Map<Long,
                    IQoS.Level> value = entry.getValue();
                value.remove(sessionIndex);
                if (value.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public long nextPackIdentity()
    {
        return 0xFFFF & _AtomicIdentity.getAndIncrement();
    }

    @Override
    public void register(ICommand<ZContext> stateMessage, long sessionIndex)
    {
        long msgId = stateMessage.getMsgId();
        if (_QttStatusMap.computeIfPresent(sessionIndex, (key, _LocalIdMessageMap) ->
        {
            IControl<ZContext> old = _LocalIdMessageMap.put(msgId, stateMessage);
            if (old == null) {
                _Logger.info("retry recv: %s", stateMessage);
            }
            return _LocalIdMessageMap;
        }) == null) {
            final Map<Long,
                      IControl<ZContext>> _LocalIdMessageMap = new HashMap<>(7);
            _LocalIdMessageMap.put(msgId, stateMessage);
            _QttStatusMap.put(sessionIndex, _LocalIdMessageMap);
            _Logger.info("first recv: %s", stateMessage);
        }
    }

    @Override
    public void ack(ICommand<ZContext> stateMessage, long sessionIndex)
    {
        int idleMax = stateMessage.getSession()
                                  .getReadTimeOutSeconds();
        long msgId = stateMessage.getMsgId();
        if (_QttStatusMap.computeIfPresent(sessionIndex, (key, old) ->
        {
            old.remove(msgId);
            return old.isEmpty() ? old
                                 : null;
        }) != null) {
            _SessionIdleQueue.offer(new Pair<>(sessionIndex, Instant.now()));
        }
        for (Iterator<IPair> it = _SessionIdleQueue.iterator(); it.hasNext();) {
            IPair pair = it.next();
            long rmSessionIndex = pair.first();
            Instant idle = pair.second();
            if (Instant.now()
                       .isAfter(idle.plusSeconds(idleMax)))
            {
                _QttStatusMap.remove(rmSessionIndex);
                it.remove();
            }
        }
    }

    @Override
    public void clean(long sessionIndex)
    {
        final Map<Long,
                  IControl<ZContext>> _LocalIdMessageMap = _QttStatusMap.remove(sessionIndex);
        if (_LocalIdMessageMap != null) {
            _LocalIdMessageMap.clear();
        }
    }
}
