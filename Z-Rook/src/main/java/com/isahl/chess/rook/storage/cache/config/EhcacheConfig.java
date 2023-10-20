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

package com.isahl.chess.rook.storage.cache.config;

import static org.ehcache.config.builders.ExpiryPolicyBuilder.timeToIdleExpiration;
import static org.ehcache.jsr107.Eh107Configuration.fromEhcacheCacheConfiguration;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.Objects;
import javax.cache.Cache;
import javax.cache.CacheManager;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author william.d.zk
 * @date 2020/6/6
 */
@Configuration
@EnableCaching
@PropertySource({ "classpath:cache.properties" })
public class EhcacheConfig
{
    private static       XmlConfiguration RookConfig;
    private static final String           DEFAULT_TEMPLATE_NAME = "rook-default";

    @PostConstruct
    private void load()
    {
        RookConfig = new XmlConfiguration(Objects.requireNonNull(getClass().getResource("/ehcache.xml")));
    }
    public static <K, V> void createCache(CacheManager cacheManager,
                                          String cacheName,
                                          Class<K> keyType,
                                          Class<V> valueType,
                                          Duration expiry) throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        CacheConfigurationBuilder<K, V> builder = RookConfig.newCacheConfigurationBuilderFromTemplate(
                DEFAULT_TEMPLATE_NAME,
                keyType,
                valueType);
        cacheManager.createCache(cacheName,
                                 fromEhcacheCacheConfiguration(builder.withExpiry(timeToIdleExpiration(expiry))
                                                                      .build()));
    }

    public void defaultDefine()
    {

    }

}
