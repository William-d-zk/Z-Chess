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

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.response.ZProgress;
import com.isahl.chess.king.base.response.ZResponse;
import com.isahl.chess.king.base.util.JsonUtil;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApplicationTest
{
    private final Logger _Logger = Logger.getLogger("z-chess.test." + getClass().getSimpleName());

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