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

package com.tgx.chess.bishop.io.mqtt.bean;

import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.io.core.async.AioContext;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

public class QttContext
        extends
        AioContext
{
    private final static IPair VERSION = new Pair<>("3.1.1", 4);

    public QttContext(ISessionOption option)
    {
        super(option);
        transfer();
    }

    private QttFrame  mCarrier;
    private CryptUtil mCryptUtil = new CryptUtil();

    public static IPair getVersion()
    {
        return VERSION;
    }

    public QttFrame getCarrier()
    {
        return mCarrier;
    }

    public void setCarrier(QttFrame carrier)
    {
        mCarrier = carrier;
    }
}
