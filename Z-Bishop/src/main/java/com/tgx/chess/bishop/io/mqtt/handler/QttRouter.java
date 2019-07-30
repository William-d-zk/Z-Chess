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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 * @date 2019-07-07
 */
public class QttRouter
        implements
        IQttRouter
{
    private final Logger                              _Logger            = Logger.getLogger(getClass().getSimpleName());
    private final Map<Pattern,
                      Map<Long,
                          IQoS.Level>>                _Topic2SessionsMap = new TreeMap<>(Comparator.comparing(Pattern::pattern));
    private final AtomicLong                          _AtomicIdentity    = new AtomicLong(Long.MIN_VALUE);
    private final Map<Long,
                      Set<Integer>>                   _QttStatusMap      = new ConcurrentHashMap<>(47);

    public Map<Long,
               IQoS.Level>

            broker(final String topic)
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
                            long index)
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
            if (value.computeIfPresent(index,
                                       (key, old) -> old.getValue() > qosLevel.getValue() ? old
                                                                                          : qosLevel) == null)
            {
                value.put(index, qosLevel);
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
    public void removeTopic(String topic, long index)
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
                value.remove(index);
                if (value.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public int nextPackIdentity()
    {
        return (int) (0xFFFF & _AtomicIdentity.getAndIncrement());
    }

    @Override
    public void register(int identity, long index)
    {
        if (_QttStatusMap.computeIfPresent(index, (key, old) ->
        {
            old.add(identity);
            return old;
        }) == null) {
            Set<Integer> identitySet = new ConcurrentSkipListSet<>();
            _QttStatusMap.put(index, identitySet);
        }
    }

    @Override
    public void ack(int identity, long index)
    {
        _QttStatusMap.computeIfPresent(index, (key, old) ->
        {
            old.remove(identity);
            return old.isEmpty() ? null
                                 : old;
        });
    }

}
