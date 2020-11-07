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

package com.isahl.chess.pawn.endpoint.spring.device.jpa.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.queen.io.core.inf.IQoS;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeviceSubscribe
        implements
        Serializable
{
    private static final long             serialVersionUID = -3075846478370159363L;
    private final Map<String,
                      IQoS.Level>         _Subscribes;

    @JsonCreator
    public DeviceSubscribe(@JsonProperty("subscribes") Map<String,
                                                           IQoS.Level> subscribes)
    {
        _Subscribes = subscribes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String,
               IQoS.Level> getSubscribes()
    {
        return _Subscribes;
    }

    public void addSubscribes(Subscribe subscribe)
    {
        if (_Subscribes.computeIfPresent(subscribe.getTopic(),
                                         (t, o) -> subscribe.getLevel()
                                                            .getValue() > o.getValue() ? subscribe.getLevel()
                                                                                       : o) == null)
        {
            _Subscribes.put(subscribe.getTopic(), subscribe.getLevel());
        }
    }

    public void clean()
    {
        if (_Subscribes != null) {
            _Subscribes.clear();
        }
    }
}
