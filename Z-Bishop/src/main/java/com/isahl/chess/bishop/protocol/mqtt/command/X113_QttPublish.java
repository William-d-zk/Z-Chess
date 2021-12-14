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
import com.isahl.chess.king.base.content.ByteBuf;

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
        generateCtrl(false, false, ALMOST_ONCE, QttType.PUBLISH);
    }

    private String mTopic;

    @Override
    public int length()
    {
        int length = 2 + (Objects.nonNull(mTopic) ? mTopic.getBytes(StandardCharsets.UTF_8).length : 0); // topic
        length += (getLevel().getValue() > ALMOST_ONCE.getValue() ? 0 : -2);//msg-id
        return super.length() + length; //payload
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_07_ROUTE_MESSAGE;
    }

    public X113_QttPublish setTopic(String topic)
    {
        mTopic = Objects.requireNonNull(topic);
        return this;
    }

    public String getTopic()
    {
        return mTopic;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int topicLength = input.getUnsignedShort();
        mTopic = input.readUTF(topicLength);
        if(getLevel().getValue() > ALMOST_ONCE.getValue()) {
            setMsgId(input.getUnsignedShort());
        }
        return input.readableBytes();
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        byte[] topicBytes = mTopic.getBytes(StandardCharsets.UTF_8);
        output.putShort((short) topicBytes.length);
        output.put(topicBytes);
        if(getLevel().getValue() > ALMOST_ONCE.getValue()) {
            output.putShort((short) getMsgId());
        }
        if(mPayload != null) {
            output.put(mPayload);
        }
        return output;
    }

    @Override
    public String toString()
    {
        return String.format("X113 publish | dup:%s,retain:%s,qos:%s | msg-id:%d topic:\"%s\" \npayload: \"%s\" \n",
                             isDuplicate(),
                             isRetain(),
                             getLevel(),
                             getMsgId(),
                             getTopic(),
                             mPayload == null ? "NULL" : new String(mPayload, StandardCharsets.UTF_8));
    }

    @Override
    public X113_QttPublish copy()
    {
        X113_QttPublish n113 = new X113_QttPublish();
        n113.setTopic(getTopic());
        n113.setLevel(getLevel());
        n113.mPayload = new byte[mPayload.length];
        System.arraycopy(mPayload, 0, n113.mPayload, 0, mPayload.length);
        return n113;
    }
}
