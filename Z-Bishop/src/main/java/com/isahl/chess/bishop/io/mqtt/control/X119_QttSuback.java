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

package com.isahl.chess.bishop.io.mqtt.control;

import static com.isahl.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.isahl.chess.bishop.io.mqtt.QttCommand;
import com.isahl.chess.bishop.io.mqtt.QttType;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-30
 */
public class X119_QttSuback
        extends
        QttCommand
{

    public final static int COMMAND = 0x119;

    public X119_QttSuback()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, ALMOST_ONCE, QttType.SUBACK));
    }

    private List<Level> mResultList;

    public void addResult(Level qosLevel)
    {
        if (mResultList == null) {
            mResultList = new LinkedList<>();
        }
        mResultList.add(qosLevel);
    }

    public List<Level> getQosLevels()
    {
        return mResultList;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength()
               + (mResultList == null ? 0
                                      : mResultList.size());
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        pos = super.decodec(data, pos);
        mResultList = new ArrayList<>(data.length - pos);
        for (int i = 0, size = data.length - pos; i < size; i++) {
            mResultList.add(Level.valueOf(data[pos++]));
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos = super.encodec(data, pos);
        if (mResultList != null) {
            for (Level qosLevel : mResultList) {
                pos += IoUtil.writeByte(qosLevel.getValue(), data, pos);
            }
        }
        return pos;
    }

    @Override
    public String toString()
    {
        return String.format("suback msg-id %d, %s", getMsgId(), getQosLevels());
    }
}
