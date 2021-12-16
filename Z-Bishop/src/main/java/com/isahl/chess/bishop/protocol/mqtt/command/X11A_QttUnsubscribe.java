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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.AT_LEAST_ONCE;

/**
 * @author william.d.zk
 * @date 2019-05-30
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x11A)
public class X11A_QttUnsubscribe
        extends QttCommand
{
    public X11A_QttUnsubscribe()
    {
        generateCtrl(false, false, AT_LEAST_ONCE, QttType.UNSUBSCRIBE);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_06_META_CREATE;
    }

    @Override
    public int length()
    {
        int length = 0;
        for(String topic : _Topics) {
            length += 2 + topic.getBytes(StandardCharsets.UTF_8).length;
        }
        return length + super.length();
    }

    private final List<String> _Topics = new ArrayList<>(3);

    public List<String> getTopics()
    {
        return _Topics;
    }

    public void setTopics(String... topics)
    {
        Collections.addAll(_Topics, topics);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        setMsgId(input.getUnsignedShort());
        while(input.isReadable()) {
            int topicLength = input.getUnsignedShort();
            String topic = input.readUTF(topicLength);
            _Topics.add(topic);
        }
        return 0;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.putShort((short) getMsgId());
        for(String topic : _Topics) {
            byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
            output.putShort((short) topicBytes.length);
            output.put(topicBytes);
        }
        return output;
    }

    @Override
    public String toString()
    {
        return String.format("unsubscribe msg-id:%d topics:%s", getMsgId(), _Topics);
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }
}
