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

package com.isahl.chess.bishop.protocol.mqtt.command;

import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-30
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x119)
public class X119_QttSuback
        extends QttCommand
{

    public X119_QttSuback()
    {
        put(generateCtrl(false, false, ALMOST_ONCE, QttType.SUBACK));
    }

    private List<Level> mResultList;

    public void addResult(Level qosLevel)
    {
        if(mResultList == null) {
            mResultList = new LinkedList<>();
        }
        mResultList.add(qosLevel);
    }

    public List<Level> getQosLevels()
    {
        return mResultList;
    }

    @Override
    public int length()
    {
        return super.length() + (mResultList == null ? 0 : mResultList.size());
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        super.decodec(input);
        mResultList = new ArrayList<>(input.remaining());
        while(input.hasRemaining()) {
            mResultList.add(Level.valueOf(input.get()));
        }
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        super.encodec(output);
        if(mResultList != null) {
            for(Level qosLevel : mResultList) {
                output.put((byte) qosLevel.getValue());
            }
        }
    }

    @Override
    public String toString()
    {
        return String.format("%#x suback msg-id %d, %s", serial(), getMsgId(), getQosLevels());
    }
}
