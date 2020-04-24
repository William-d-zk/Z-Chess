/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tgx.chess.knight.cluster.spring.service;

import org.springframework.stereotype.Component;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.event.handler.cluster.INotifyCustom;
import com.tgx.chess.queen.io.core.inf.IProtocol;

@Component
public class ConsistentCustom
        implements
        INotifyCustom
{
    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    @Override
    public Void handle(IProtocol iProtocol, Void aVoid)
    {
        _Logger.info("notify---consistent");
        return null;
    }

    @Override
    public String getName()
    {
        return "operator.consistent";
    }
}
