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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-30
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x113)
public class X113_QttPublish
        extends QttCommand
{

    public X113_QttPublish()
    {
        put(generateCtrl(false, false, ALMOST_ONCE, QttType.PUBLISH));
    }

    private String mTopic;

    @Override
    public int length()
    {
        return super.length() + 2 + (Objects.nonNull(mTopic) ? mTopic.getBytes(StandardCharsets.UTF_8).length : 0) +
               (getLevel().getValue() > ALMOST_ONCE.getValue() ? 0 : -2);
    }

    public void setTopic(String topic)
    {
        Objects.requireNonNull(topic);
        this.mTopic = topic;
    }

    public String getTopic()
    {
        return mTopic;
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        int topicSize = input.getShort() & 0xFFFF;
        mTopic = new String(input.array(), input.position(), topicSize, StandardCharsets.UTF_8);
        input.position(input.position() + topicSize);
        if(getLevel().getValue() > ALMOST_ONCE.getValue()) {
            super.decodec(input);
        }
        mPayload = new byte[input.remaining()];
        input.get(mPayload);
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        byte[] topicBytes = mTopic.getBytes(StandardCharsets.UTF_8);
        output.putShort((short) topicBytes.length);
        output.put(topicBytes);
        if(getLevel().getValue() > ALMOST_ONCE.getValue()) {
            super.encodec(output);
        }
        output.put(mPayload);
    }

    @Override
    public String toString()
    {
        return String.format("[X113 publish | dup:%s,retain:%s,qos:%s | msg-id:%d topic:\"%s\" payload: \"%s\" ]",
                             isDuplicate(),
                             isRetain(),
                             getLevel(),
                             getMsgId(),
                             getTopic(),
                             mPayload == null ? "NULL" : new String(mPayload, StandardCharsets.UTF_8));
    }

    @Override
    public X113_QttPublish duplicate()
    {
        X113_QttPublish n113 = new X113_QttPublish();
        n113.setTopic(getTopic());
        n113.setLevel(getLevel());
        n113.put(mPayload);
        return n113;
    }
}
