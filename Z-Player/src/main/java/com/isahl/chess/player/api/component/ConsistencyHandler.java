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

package com.isahl.chess.player.api.component;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.events.cluster.IConsistencyHandler;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ConsistencyHandler
        implements IConsistencyHandler
{

    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    @Override
    public boolean onConsistencyCall(IConsistent result)
    {
        String json = new String(result.encode()
                                       .array(), StandardCharsets.UTF_8);
        _Logger.debug("consistency %s", json);
        return false;
    }
}
