/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.audience.client.config;

import jakarta.annotation.PostConstruct;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.lang.reflect.Field;

/**
 * Z-Audience 缓存配置类
 * 用于初始化 EhcacheConfig 中的静态配置并提供 CacheManager
 */
@Configuration
@EnableCaching
public class AudienceCacheConfig {

    @PostConstruct
    public void init() throws Exception {
        // 加载 ehcache.xml 并通过反射设置 EhcacheConfig.RookConfig
        XmlConfiguration config = new XmlConfiguration(getClass().getResource("/ehcache.xml"));
        
        // 获取 EhcacheConfig 类并设置静态字段
        try {
            Class<?> ehcacheConfigClass = Class.forName("com.isahl.chess.rook.storage.cache.config.EhcacheConfig");
            Field rookConfigField = ehcacheConfigClass.getDeclaredField("RookConfig");
            rookConfigField.setAccessible(true);
            rookConfigField.set(null, config);
        } catch (ClassNotFoundException e) {
            // EhcacheConfig 类不存在，忽略
        }
    }

    @Bean
    @Primary
    public CacheManager jcacheCacheManager() {
        CachingProvider provider = Caching.getCachingProvider();
        return provider.getCacheManager();
    }
}
