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

import com.isahl.chess.king.base.log.Logger;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.cache.CacheManager;
import java.net.URL;
import java.util.Objects;

/**
 * @author william.d.zk
 * @date 2020/6/6
 */
@Configuration
@EnableCaching
@PropertySource({"classpath:cache.properties"})
public class EhcacheConfig
{

    private final Logger _Logger = Logger.getLogger("rook.cache." + getClass().getSimpleName());

    private final CacheManager _CacheManager;

    /*    
    
    //    private final CacheEventListener _Listener;
    
    @Autowired
    public EhcacheConfig()
    {
        URL ehcaheUrl = Objects.requireNonNull(getClass().getResource("/ehcache.xml"));
        XmlConfiguration xmlConfiguration = new XmlConfiguration(ehcaheUrl);
        _CacheManager = CacheManagerBuilder.newCacheManager(xmlConfiguration);
        _CacheManager.init();
        org.ehcache.config.Configuration configuration = _CacheManager.getRuntimeConfiguration();
    }
    
    @Bean
    public CacheManager getCacheManager()
    {
        return _CacheManager;
    }
    
    @Bean
    public Cache<Integer,
                 Double> getCache0()
    {
        return _CacheManager.getCache("areaOfCircleCache", Integer.class, Double.class);
    }
    */
    @Autowired
    public EhcacheConfig(CacheManager cm) throws ClassNotFoundException,
                                          InstantiationException,
                                          IllegalAccessException
    {
        _CacheManager = cm;
        URL ehcaheUrl = Objects.requireNonNull(getClass().getResource("/ehcache.xml"));
        XmlConfiguration xmlConfiguration = new XmlConfiguration(ehcaheUrl);
        CacheConfigurationBuilder<Integer,
                                  Double> builder = xmlConfiguration.newCacheConfigurationBuilderFromTemplate("rook-default",
                                                                                                              Integer.class,
                                                                                                              Double.class);

        _CacheManager.createCache("cache0", Eh107Configuration.fromEhcacheCacheConfiguration(builder.build()));
    }
}
