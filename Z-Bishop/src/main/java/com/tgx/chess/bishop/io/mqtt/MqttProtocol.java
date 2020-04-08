/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess
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

package com.tgx.chess.bishop.io.mqtt;

import static com.tgx.chess.bishop.io.Direction.CLIENT_TO_SERVER;
import static com.tgx.chess.bishop.io.Direction.SERVER_TO_CLIENT;

import java.util.Objects;

import com.tgx.chess.bishop.io.Direction;
import com.tgx.chess.queen.io.core.inf.IDuplicate;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 * @date 2019-05-25
 */
public abstract class MqttProtocol
        implements
        IProtocol,
        IQoS,
        IDuplicate
{
    private final static byte DUPLICATE_FLAG = 1 << 3;
    private final static byte RETAIN_FLAG    = 1;
    private final static byte QOS_MASK       = 3 << 1;

    public enum QTT_TYPE
    {

        CONNECT(1, "Client request to connect to Server", CLIENT_TO_SERVER),
        CONNACK(2, "Connect acknowledgment", SERVER_TO_CLIENT),
        PUBLISH(3, "Publish message", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
        PUBACK(4, "Publish acknowledgment", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
        PUBREC(5, "Publish received (assured delivery part 1)", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
        PUBREL(6, "Publish release (assured delivery part 2)", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
        PUBCOMP(7, "Publish complete (assured delivery part 3)", CLIENT_TO_SERVER, SERVER_TO_CLIENT),
        SUBSCRIBE(8, "Client subscribe request", CLIENT_TO_SERVER),
        SUBACK(9, "Subscribe acknowledgment", SERVER_TO_CLIENT),
        UNSUBSCRIBE(10, "Unsubscribe request", CLIENT_TO_SERVER),
        UNSUBACK(11, "Unsubscribe acknowledgment", SERVER_TO_CLIENT),
        PINGREQ(12, "PING request", CLIENT_TO_SERVER),
        PINGRESP(13, "PING response", SERVER_TO_CLIENT),
        DISCONNECT(14, "Client is disconnecting", CLIENT_TO_SERVER);

        final int         _Value;
        final String      _Description;
        final Direction[] _Directions;

        QTT_TYPE(int code,
                 String description,
                 Direction... directions)
        {
            _Value = code << 4;
            _Directions = directions;
            _Description = description;
        }

        public final int getValue()
        {
            return _Value;
        }

        public Direction[] getDirections()
        {
            return _Directions;
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

    byte             frame_op_code;
    private boolean  duplicate;
    private boolean  retain;
    private byte     qos_level;
    private QTT_TYPE type;

    private void checkOpCode()
    {
        if (getLevel() == Level.ALMOST_ONCE && duplicate) {
            throw new IllegalStateException("level == 0 && duplicate");
        }
    }

    public static byte generateCtrl(boolean dup, boolean retain, Level qosLevel, QTT_TYPE qttType)
    {
        byte ctrl = 0;
        ctrl |= dup ? DUPLICATE_FLAG
                    : 0;
        ctrl |= retain ? RETAIN_FLAG
                       : 0;
        ctrl |= qosLevel.ordinal() << 1;
        ctrl |= qttType.getValue();
        return ctrl;
    }

    public void setDuplicate(boolean duplicate)
    {
        this.duplicate = duplicate;
        if (duplicate) {
            frame_op_code |= DUPLICATE_FLAG;
        }
        else {
            frame_op_code &= ~DUPLICATE_FLAG;
        }
    }

    @Override
    public boolean isDuplicate()
    {
        return duplicate;
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

    public void setLevel(Level level)
    {
        qos_level = (byte) level.ordinal();
        frame_op_code &= ~QOS_MASK;
        frame_op_code |= qos_level << 1;
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
        duplicate = (frame_op_code & DUPLICATE_FLAG) == DUPLICATE_FLAG;
        retain = (frame_op_code & RETAIN_FLAG) == RETAIN_FLAG;
        qos_level = (byte) ((frame_op_code & QOS_MASK) >> 1);
        checkOpCode();
    }

    byte getOpCode()
    {
        return frame_op_code;
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    public IQoS.Level getLevel()
    {
        return IQoS.Level.valueOf(qos_level);
    }

}
