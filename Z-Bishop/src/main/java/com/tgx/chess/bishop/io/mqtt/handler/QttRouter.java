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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.tgx.chess.bishop.io.mqtt.bean.BaseQtt;
import com.tgx.chess.king.base.util.Pair;

/**
 * @author william.d.zk
 * @date 2019-07-07
 */
public class QttRouter
        implements
        IQttRouter
{

    private final Map<Pattern,
                      Pair<BaseQtt.QOS_LEVEL,
                           Set<Long>>> _Topic2SessionsMap = new TreeMap<>(Comparator.comparing(Pattern::pattern));

    @PostConstruct
    private void addDefault()
    {

    }

    public Set<Pair<BaseQtt.QOS_LEVEL,
                    Long>> broker(final String topic)
    {
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
                                 .flatMap(pair ->
                                 {
                                     Set<Long> indexSet = pair.second();
                                     BaseQtt.QOS_LEVEL qosLevel = pair.first();
                                     return indexSet.stream()
                                                    .map(l -> new Pair<>(qosLevel, l));
                                 })
                                 .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    public void addTopic(Pair<String,
                              BaseQtt.QOS_LEVEL> pair,
                         long index)
    {
        String topic = pair.first();
        BaseQtt.QOS_LEVEL qosLevel = pair.second();
        Pattern pattern = topic.endsWith("/+") ? Pattern.compile(topic.replace("+", "[^/]+"))
                                               : topic.endsWith("/#") ? Pattern.compile(topic.replace("#", ".+"))
                                                                      : Pattern.compile(topic);
        Pair<BaseQtt.QOS_LEVEL,
             Set<Long>> value = _Topic2SessionsMap.get(pattern);
        if (Objects.isNull(value)) {
            value = new Pair<>(qosLevel, new ConcurrentSkipListSet<>());
        }
        value.second()
             .add(index);
    }

}
