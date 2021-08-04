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

package com.isahl.chess.bishop.io.mqtt;

import com.isahl.chess.bishop.io.ws.zchat.ZContext;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.inf.INetworkOption;
import com.isahl.chess.queen.io.core.inf.ISort;

import java.util.stream.IntStream;

import static com.isahl.chess.bishop.io.mqtt.MqttProtocol.VERSION_V3_1_1;
import static com.isahl.chess.bishop.io.mqtt.MqttProtocol.VERSION_V5_0;
import static com.isahl.chess.king.base.schedule.inf.ITask.*;
import static com.isahl.chess.queen.io.core.inf.ISession.CAPACITY;

/**
 * @author william.d.zk
 */
public class QttContext
        extends ZContext
{
    private final static IPair SUPPORT_VERSION = new Pair<>(new String[]{ "5.0", "3.1.1" },
                                                            new int[]{ VERSION_V5_0, VERSION_V3_1_1 });

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
        advanceState(_DecodeState, DECODE_FRAME, CAPACITY);
        advanceState(_EncodeState, ENCODE_FRAME, CAPACITY);
    }

    /*MQTT 协议对filter chain来说只有一个in frame 阶段所以 override isInFrame 与 outInFrame
     * 避免filter 中 seek 和peek 方法多次判断 InFrame & InConvert OutFrame & OutConvert状态
     * */
    @Override
    public boolean isInFrame()
    {
        int state = _DecodeState.get();
        return stateAtLeast(state, DECODE_FRAME) && stateLessThan(state, DECODE_ERROR);
    }

    @Override
    public boolean isOutFrame()
    {
        int state = _EncodeState.get();
        return stateAtLeast(state, ENCODE_FRAME) && stateLessThan(state, ENCODE_ERROR);
    }

}
