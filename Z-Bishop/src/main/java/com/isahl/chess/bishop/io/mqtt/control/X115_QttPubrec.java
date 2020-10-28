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

package com.isahl.chess.bishop.io.mqtt.control;

import static com.isahl.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;

import com.isahl.chess.bishop.io.mqtt.QttCommand;
import com.isahl.chess.queen.io.core.inf.IPContext;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-30
 */
public class X115_QttPubrec
        extends
        QttCommand
{
    public final static int COMMAND = 0x119;

    public X115_QttPubrec()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, ALMOST_ONCE, QTT_TYPE.PUBREC));
    }

    @Override
    public String toString()
    {
        return String.format("x115 pubrec:{local-id:%d}", getLocalId());
    }
}
