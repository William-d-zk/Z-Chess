/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.arena.gateway.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.cache.CacheManager;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class RookCacheService
{
    private final Logger _Logger = Logger.getLogger("arena." + getClass().getSimpleName());

    @Cacheable(value = "areaOfCircleCache",
               key = "#radius",
               condition = "#radius > 5")
    public double areaOfCircle(int radius)
    {
        _Logger.info("calculate the area of a circle with a radius of {}", radius);
        return Math.PI * Math.pow(radius, 2);
    }

    @Cacheable(value = "cache0",
               key = "#radius",
               condition = "#radius > 5")
    public double cache0(int radius)
    {
        _Logger.info("calculate the area of a circle with a radius of {}", radius);
        return Math.PI * Math.pow(radius, 2);
    }

    final CacheManager _CacheManager;

    @Autowired
    public RookCacheService(CacheManager cacheManager)
    {
        _CacheManager = cacheManager;
    }

    @PostConstruct
    void post() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager, "cache0", Integer.class, Double.class, Duration.of(20, MINUTES));
    }
}
