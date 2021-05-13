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

package com.isahl.chess.pawn.endpoint.device.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceSubscribe;
import com.isahl.chess.pawn.endpoint.device.jpa.model.Subscribe;
import com.isahl.chess.queen.io.core.inf.IQoS;

class DurableServiceTest
{
    @Test
    public void testList2Map() throws JsonProcessingException
    {
        Map<String,
            IQoS.Level> subscribes = new HashMap<>();
        DeviceSubscribe subscribe = new DeviceSubscribe(subscribes);
        subscribe.subscribe(new Subscribe(IQoS.Level.ALMOST_ONCE, "topic0"));
        subscribe.subscribe(new Subscribe(IQoS.Level.EXACTLY_ONCE, "topic1"));
        subscribe.subscribe(new Subscribe(IQoS.Level.AT_LEAST_ONCE, "topic2"));
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(subscribe));
    }

    @Test
    public void testJson() throws JsonProcessingException
    {
        String json = "{\"subscribes\":{\"topic1\":\"EXACTLY_ONCE\",\"topic2\":\"AT_LEAST_ONCE\",\"topic0\":\"ALMOST_ONCE\"}}";
        ObjectMapper mapper = new ObjectMapper();
        DeviceSubscribe subscribe = mapper.readValue(json, DeviceSubscribe.class);
        System.out.println(subscribe);
    }
}