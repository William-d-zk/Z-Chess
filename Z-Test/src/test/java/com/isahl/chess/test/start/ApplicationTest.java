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

package com.isahl.chess.test.start;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.response.ZProgress;
import com.isahl.chess.king.base.response.ZResponse;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.knight.json.JsonUtil;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.DeviceSubscribe;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IDeviceJpaRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApplicationTest
{
    private final Logger _Logger = Logger.getLogger("z-chess.test." + getClass().getSimpleName());

    @Autowired
    private JsonUtil             _JsonUtil;
    @Autowired
    private IDeviceJpaRepository _DeviceJpaRepository;

    @Test
    public void testZProgress()
    {
        ZProgress progress = new ZProgress(100);
        String json = JsonUtil.writeValueAsString(progress);
        _Logger.info(json);
        ZProgress read = JsonUtil.readValue(json, ZProgress.class);
        _Logger.info("test end");
    }

    @Test
    public void testZResponse()
    {
        ZResponse<Void> noDetail = ZResponse.success(null);
        System.out.println(JsonUtil.writeValueAsString(noDetail));
    }

    @Test
    public void testDeviceRepository()
    {
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setSn("test000-10001-001202123");
        deviceEntity.setUsername("test1234-A");
        deviceEntity.setPassword("88712390087654dfrtyiu0-123");
        deviceEntity.setPasswordId(0);
        deviceEntity.setInvalidAt(LocalDateTime.now()
                                               .plusDays(41));
        deviceEntity.setToken(CryptUtil.SHA256("test"));
        DeviceSubscribe subscribe = new DeviceSubscribe(new HashMap<>());
        // subscribe.addSubscribes(new Subscribe(IQoS.Level.EXACTLY_ONCE, "test#"));
        deviceEntity.setSubscribe(subscribe);
        DeviceEntity exist = _DeviceJpaRepository.findByToken(deviceEntity.getToken());
        if (exist != null) deviceEntity.setId(exist.getId());
        _DeviceJpaRepository.save(deviceEntity);
    }

    @Test
    public void output()
    {
        JsonNode jsonOutput = JsonUtil.readTree(getClass().getResourceAsStream("/output.json"));
        System.out.println(jsonOutput);
        JsonNode vehicleTypeWithRouteList = jsonOutput.get("vehicleTypeWithRouteList");
        for (JsonNode e : vehicleTypeWithRouteList) {
            JsonNode jobList = e.get("jobList");
            for (JsonNode job : jobList) {
                JsonNode attrs = job.get("attrs");
                JsonNode st = attrs.get("service_time");
                JsonNode readyTime = attrs.get("ready_time");
                JsonNode dueTime = attrs.get("due_time");
                JsonNode width = attrs.get("width");
                JsonNode height = attrs.get("height");
                System.out.println("ready_time:" + LocalTime.ofSecondOfDay(readyTime.asLong() / 1000));
                System.out.println("due_time:" + LocalTime.ofSecondOfDay(dueTime.asLong() / 1000));
            }
        }
    }
}