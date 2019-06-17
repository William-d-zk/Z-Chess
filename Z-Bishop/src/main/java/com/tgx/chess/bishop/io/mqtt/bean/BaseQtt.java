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

import java.util.Objects;

import com.tgx.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 * @date 2019-05-25
 */
public class BaseQtt
{
    private final static byte DUPLICATE_FLAG    = 1 << 3;
    private final static byte QOS_ONLY_ONCE     = 2;
    private final static byte QOS_AT_LEAST_ONCE = 1;
    private final static byte QOS_LESS_ONCE     = 0;
    private final static byte RETAIN_FLAG       = 1;
    private final static byte QOS_MASK          = 0x06;

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

        final int    _Value;
        final String _Description;
        final String _Direction;

        QTT_TYPE(int code,
                 String direction,
                 String description)
        {
            _Value = code << 4;
            _Direction = direction;
            _Description = description;
        }

        public final int getValue()
        {
            return _Value;
        }

        public String getDirection()
        {
            return _Direction;
        }

        public String getDescription()
        {
            return _Description;
        }

        public static QTT_TYPE valueOf(int head)
        {
            switch (head & 240)
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
                case 8 << 4:
                    return SUBSCRIBE;
                case 9 << 4:
                    return SUBACK;
                case 10 << 4:
                    return UNSUBSCRIBE;
                case 11 << 4:
                    return UNSUBACK;
                case 12 << 4:
                    return PINGREQ;
                case 13 << 4:
                    return PINGRESP;
                case 14 << 4:
                    return DISCONNECT;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public enum QOS_LEVEL
    {
        QOS_ONLY_ONCE(BaseQtt.QOS_ONLY_ONCE),
        QOS_LESS_ONCE(BaseQtt.QOS_LESS_ONCE),
        QOS_AT_LEAST_ONCE(BaseQtt.QOS_AT_LEAST_ONCE);

        final byte _Value;

        public byte getValue()
        {
            return _Value;
        }

        QOS_LEVEL(byte level)
        {
            _Value = level;
        }

        public static QOS_LEVEL valueOf(byte level)
        {
            switch (level)
            {
                case BaseQtt.QOS_LESS_ONCE:
                    return QOS_LESS_ONCE;
                case BaseQtt.QOS_AT_LEAST_ONCE:
                    return QOS_AT_LEAST_ONCE;
                case BaseQtt.QOS_ONLY_ONCE:
                    return QOS_ONLY_ONCE;
                default:
                    throw new IllegalArgumentException("QoS reserved");
            }
        }
    }

    byte             frame_op_code;
    private boolean  dup;
    private boolean  retain;
    private byte     qos_level;
    private QTT_TYPE type;

    public static byte generateCtrl(boolean dup, boolean retain, QOS_LEVEL qosLevel, QTT_TYPE qttType)
    {
        byte ctrl = 0;
        ctrl |= dup ? DUPLICATE_FLAG
                    : 0;
        ctrl |= retain ? RETAIN_FLAG
                       : 0;
        ctrl |= qosLevel.getValue() << 1;
        ctrl |= qttType.getValue();
        return ctrl;
    }

    public void setDup(boolean dup)
    {
        this.dup = dup;
        if (dup) {
            frame_op_code |= DUPLICATE_FLAG;
        }
        else {
            frame_op_code &= ~DUPLICATE_FLAG;
        }
    }

    public boolean isDup()
    {
        return dup;
    }

    public void setRetain(boolean retain)
    {
        this.retain = retain;
        if (retain) {
            frame_op_code |= RETAIN_FLAG;
        }
        else {
            frame_op_code &= ~RETAIN_FLAG;
        }
    }

    public boolean isRetain()
    {
        return retain;
    }

    public void setQosLevel(QOS_LEVEL level)
    {
        qos_level = level.getValue();
        frame_op_code &= ~QOS_MASK;
        frame_op_code |= qos_level << 1;
    }

    public QOS_LEVEL getQosLevel()
    {
        return QOS_LEVEL.valueOf(qos_level);
    }

    public QTT_TYPE getType()
    {
        return type;
    }

    public void setType(QTT_TYPE type)
    {
        this.type = type;
        frame_op_code |= type.getValue();
    }

    void setOpCode(byte opCode)
    {
        frame_op_code = opCode;
        type = QTT_TYPE.valueOf(getOpCode());
        if (Objects.isNull(type)) { throw new IllegalArgumentException(); }
        dup = (frame_op_code & DUPLICATE_FLAG) == DUPLICATE_FLAG;
        retain = (frame_op_code & RETAIN_FLAG) == RETAIN_FLAG;
        qos_level = (byte) ((frame_op_code & QOS_MASK) >> 1);
    }

    byte getOpCode()
    {
        return frame_op_code;
    }

    public IQoS.Level convertQosLevel()
    {
        switch (getQosLevel())
        {
            case QOS_LESS_ONCE:
                return IQoS.Level.LESS_ONCE;
            case QOS_AT_LEAST_ONCE:
                return IQoS.Level.AT_LEAST_ONCE;
            default:
                return IQoS.Level.ONLY_ONCE;
        }
    }

}
