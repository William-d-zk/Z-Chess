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

package com.isahl.chess.bishop.protocol.mqtt.model;

import com.isahl.chess.bishop.protocol.ProtocolContext;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.ISort;

import java.util.stream.IntStream;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V3_1_1;
import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;

/**
 * @author william.d.zk
 */
public class QttContext
        extends ProtocolContext<QttFrame>
{
    private final static IPair SUPPORT_VERSION = new Pair<>(new String[]{
            "5.0",
            "3.1.1"
    }, new int[]{
            VERSION_V5_0,
            VERSION_V3_1_1
    });

    public QttContext(INetworkOption option, ISort.Mode mode, ISort.Type type)
    {
        super(option, mode, type);
    }

    public static IPair getSupportVersion()
    {
        return SUPPORT_VERSION;
    }

    public static boolean isNoSupportVersion(int version)
    {
        int[] supportedVersions = SUPPORT_VERSION.getSecond();
        return IntStream.of(supportedVersions)
                        .noneMatch(v->v == version);
    }

    private int mVersion;

    public void setVersion(int version)
    {
        mVersion = version;
    }

    public int getVersion()
    {
        return mVersion;
    }

    @Override
    public void ready()
    {
        advanceOutState(ENCODE_PAYLOAD);
        advanceInState(DECODE_FRAME);
    }
}
