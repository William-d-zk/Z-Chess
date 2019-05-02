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

import java.nio.ByteBuffer;
import java.util.Objects;

import com.tgx.chess.king.base.inf.IReset;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.inf.IFrame;

/**
 * @author william.d.zk
 * @date 2019-05-02
 */
public class QttFrame
        implements
        IReset,
        IFrame
{

    @Override
    public boolean isNoCtrl()
    {
        int head = frame_op_code & 240;
        return head == QTT_TYPE.CONNECT._Value
               || head == QTT_TYPE.CONNACK._Value
               || head == QTT_TYPE.PINGREQ._Value
               || head == QTT_TYPE.PINGRESP._Value
               || head == QTT_TYPE.DISCONNECT._Value;
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        frame_op_code = ctrl;
    }

    @Override
    public void setPayload(byte[] payload)
    {

    }

    @Override
    public void reset()
    {
        mPayloadLength = -1;
        frame_op_code = 0;
    }

    public enum QTT_TYPE
    {

        CONNECT(1, "C->S", "Client request to connect to Server"),
        CONNACK(2, "S->C", "Connect acknowledgment"),
        PUBLISH(3, "C->S | S->C", "Publish message"),
        PUBACK(4, "C->S | S->C", "Publish acknowledgment"),
        PUBREC(5, "C->S | S->C", "Publish received (assured delivery part 1)"),
        PUBREL(6, "C->S | S->C", "Publish release (assured delivery part 2)"),
        PUBCOMP(7, "C->S | S->C", "Publish complete (assured delivery part 3)"),
        SUBSCRIBE(8, "C->S", "Client subscribe request"),
        SUBACK(9, "S->C", "Subscribe acknowledgment"),
        UNSUBSCRIBE(10, "C->S", "Unsubscribe request"),
        UNSUBACK(11, "S->C", "Unsubscribe acknowledgment"),
        PINGREQ(12, "C->S", "PING request"),
        PINGRESP(13, "S->C", "PING response"),
        DISCONNECT(14, "C->S", "Client is disconnecting");

        final byte   _Value;
        final String _Description;
        final String _Flow;

        QTT_TYPE(int code,
                 String flow,
                 String description)
        {
            _Value = (byte) (code << 4);
            _Flow = flow;
            _Description = description;
        }

        public byte getValue()
        {
            return _Value;
        }

        public String getFlow()
        {
            return _Flow;
        }

        public String getDescription()
        {
            return _Description;
        }

        static QTT_TYPE valueOf(byte head)
        {
            switch (head)
            {
                case 1 << 4:
                    return CONNECT;
                case 2 << 4:
                    return CONNACK;
                case 3 << 4:
                    return PUBLISH;
                case 4 << 4:
                    return PUBACK;
                case 5 << 4:
                    return PUBREC;
                case 6 << 4:
                    return PUBREL;
                case 7 << 4:
                    return PUBCOMP;
                case (byte) (8 << 4):
                    return SUBSCRIBE;
                case (byte) (9 << 4):
                    return SUBACK;
                case (byte) (10 << 4):
                    return UNSUBSCRIBE;
                case (byte) (11 << 4):
                    return UNSUBACK;
                case (byte) (12 << 4):
                    return PINGREQ;
                case (byte) (13 << 4):
                    return PINGRESP;
                case (byte) (14 << 4):
                    return DISCONNECT;
            }
            return null;
        }

    }

    final static byte duplicate_flag    = 1 << 3;
    final static byte qos_only_once     = 2 << 1;
    final static byte qos_at_least_once = 1 << 1;
    final static byte qos_less_once     = 0;
    final static byte retain_flag       = 1;
    final static byte qos_mask          = 0x06;

    private final static int MQTT_FRAME = FRAME_SERIAL + 2;

    private int mPayloadLength;

    @Override
    public int dataLength()
    {
        return 1 + mPayloadLength + mPayloadLength < 128 ? 1
                                                         : mPayloadLength < 16384 ? 2
                                                                                  : mPayloadLength < 2097152 ? 3
                                                                                                             : 4;
    }

    @Override
    public int getSerial()
    {
        return MQTT_FRAME;
    }

    private byte     frame_op_code;
    private boolean  dup;
    private boolean  retain;
    private byte     qos_level;
    private QTT_TYPE type;

    @Override
    public int decodec(byte[] data, int pos)
    {
        byte head = data[pos++];
        type = QTT_TYPE.valueOf((byte) (head & 240));
        if (Objects.isNull(type)) { throw new IllegalArgumentException(); }
        dup = (head & duplicate_flag) == duplicate_flag;
        retain = (head & retain_flag) == retain_flag;
        qos_level = (byte) (head & qos_mask);
        mPayloadLength = (int) IoUtil.readVariableLongLength(ByteBuffer.wrap(data, pos, data.length - pos));
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        return 0;
    }
}
