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

package com.isahl.chess.rook.storage.cache.ehcache;

import com.isahl.chess.king.base.log.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * @author william.d.zk
 * @date 2020/6/6
 */
public class CacheLogger
        implements CacheEventListener<Object, Object>
{
    private final Logger _Logger = Logger.getLogger("rook.ehcache." + getClass().getSimpleName());

    @Override
    public void onEvent(CacheEvent<?, ?> cacheEvent)
    {
        _Logger.info("Rook-Cache Key: {%s} | EventType: {%s} | Old value: {%s} | New value: {%s}",
                     cacheEvent.getKey(),
                     cacheEvent.getType(),
                     cacheEvent.getOldValue(),
                     cacheEvent.getNewValue());
    }
}
