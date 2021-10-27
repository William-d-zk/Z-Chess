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

package com.isahl.chess.pawn.endpoint.device.db.generator;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @author william.d.zk
 * @date 2021/5/15
 */
public class MessageCacheKeyGenerator
        implements KeyGenerator
{

    @Override
    public final Object generate(Object target, Method method, Object... params)
    {
        String _KeyPrefix = "mc0_";
        StringBuilder sb = new StringBuilder();
        sb.append(_KeyPrefix);
        sb.append(target.getClass()
                        .getSimpleName());
        sb.append('_');
        sb.append(method.getName());
        if(params.length > 0) {
            for(Object param : params) {
                sb.append(param.getClass()
                               .getSimpleName())
                  .append(':')
                  .append(param)
                  .append('_');
            }
        }
        else {
            sb.append('0');
        }
        return sb.toString();
    }
}
