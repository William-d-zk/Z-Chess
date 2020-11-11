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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.isahl.chess.bishop.io.mqtt.QttCommand;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-30
 */
public class X113_QttPublish
        extends
        QttCommand
{
    public final static int COMMAND = 0x113;

    public X113_QttPublish()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, ALMOST_ONCE, QTT_TYPE.PUBLISH));
    }

    private String mTopic;

    @Override
    public int dataLength()
    {
        return (getLevel().ordinal() > ALMOST_ONCE.ordinal() ? super.dataLength()
                                                             : Objects.nonNull(getPayload()) ? getPayload().length
                                                                                             : 0)
               + 2
               + (Objects.nonNull(mTopic) ? mTopic.getBytes(StandardCharsets.UTF_8).length
                                          : 0);
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
    public int decodec(byte[] data, int pos)
    {
        int topicSize = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        mTopic = new String(data, pos, topicSize, StandardCharsets.UTF_8);
        pos += topicSize;
        if (getLevel().ordinal() > ALMOST_ONCE.ordinal()) {
            pos = super.decodec(data, pos);
        }
        setPayload(new byte[data.length - pos]);
        pos = IoUtil.read(data, pos, getPayload());
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        byte[] topicBytes = mTopic.getBytes(StandardCharsets.UTF_8);
        pos += IoUtil.writeShort(topicBytes.length, data, pos);
        pos += IoUtil.write(topicBytes, data, pos);
        if (getLevel().ordinal() > ALMOST_ONCE.ordinal()) {
            pos = super.encodec(data, pos);
        }
        pos += IoUtil.write(getPayload(), data, pos);
        return pos;
    }

    @Override
    public String toString()
    {
        return String.format("[publish | dup:%s,retain:%s,qos:%s | local-id:%d topic:\"%s\" payload: \"%s\" ]",
                             isDuplicate(),
                             isRetain(),
                             getLevel(),
                             getMsgId(),
                             getTopic(),
                             getPayload() == null ? "NULL"
                                                  : new String(getPayload(), StandardCharsets.UTF_8));
    }
}
