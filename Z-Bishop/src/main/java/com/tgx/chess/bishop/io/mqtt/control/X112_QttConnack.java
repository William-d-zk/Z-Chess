/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.bishop.io.mqtt.control;

import com.tgx.chess.bishop.io.mqtt.bean.QttControl;
import com.tgx.chess.bishop.io.mqtt.bean.QttFrame;

/**
 * @author william.d.zk
 * @date 2019-05-11
 */
public class X112_QttConnack
        extends
        QttControl
{
    public final static int   COMMAND = 0x112;
    private final static byte CTRL    = QttFrame.generateCtrl(false, false, QttFrame.QOS_LEVEL.QOS_ONLY_ONCE, QttFrame.QTT_TYPE.CONNACK);

    public X112_QttConnack() {
        super(COMMAND);
        setCtrl(CTRL);
    }

    @Override
    public int dataLength() {
        return 0;
    }

    @Override
    public int getPriority() {
        return QOS_00_NETWORK_CONTROL;
    }
}
