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
package com.isahl.chess.bishop.protocol.ws.model;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.util.Objects;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_FRAME_SERIAL)
public class WsFrame
        implements IFrame
{
    public final static byte frame_op_code_ctrl_handshake = 0x00;
    public final static byte frame_op_code_ctrl_text      = 0x01;
    public final static byte frame_op_code_ctrl_binary    = 0x02;
    public final static byte frame_op_code_ctrl_close     = 0x08;
    public final static byte frame_op_code_ctrl_ping      = 0x09;
    public final static byte frame_op_code_ctrl_pong      = 0x0A;
    public final static byte frame_op_code_mask           = 0x0F;
    public final static byte frame_fin_more               = 0x00;
    public final static int  frame_fin_no_more            = 0x80;
    public final static byte frame_rsv_1_mask             = 0x40;
    public final static byte frame_rsv_2_mask             = 0x20;
    public final static byte frame_rsv_3_mask             = 0x10;

    public  byte     mFrameHeader;
    private byte[]   mMask;
    private byte[]   mPayload;
    // mask | first payload_length
    private IoSerial mSubContent;

    public boolean isFrameFin()
    {
        return (mFrameHeader & 0x80) != 0;
    }

    public boolean isFrameMore()
    {
        return (mFrameHeader & 0x80) == 0;
    }

    public void doMask()
    {
        if(mMask != null && mPayload != null) {
            for(int i = 0; i < mPayload.length; i++) {mPayload[i] ^= mMask[i & 3];}
        }
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    public void setMask(byte[] mask)
    {
        mMask = mask;
    }

    @Override
    public int length()
    {
        int length = 1 + // header
                     (mMask == null ? 0 : 4) + //mask
                     1; // attr
        if(mPayload != null) {
            if(mPayload.length > 0xFFFF) {
                length += 8;
            }
            else if(mPayload.length > 0x7D) {
                length += 2;
            }
            length += mPayload.length;
        }
        return length;
    }

    @Override
    public int sizeOf()
    {
        return length();
    }

    public boolean checkRSV(byte value)
    {
        return (value & frame_rsv_1_mask) == 0 && (value & frame_rsv_2_mask) == 0 && (value & frame_rsv_3_mask) == 0;
    }

    @Override
    public void header(int header)
    {
        mFrameHeader = (byte) header;
    }

    @Override
    public byte header()
    {
        return mFrameHeader;
    }

    @Override
    public boolean isCtrl()
    {
        return (mFrameHeader & 0x08) != 0;
    }

    @Override
    public int lack(ByteBuf input)
    {
        int remain = input.readableBytes();
        if(remain < 2) {
            return 1;
        }
        else {
            header(input.peek(0));
            int code = input.peek(1);
            int lack = code & 0x7F;
            boolean withMask = (code & 0x80) > 0;
            switch(lack) {
                case 0x7F -> {
                    int target = withMask ? 14 : 10;
                    if(remain < target) {
                        return target - remain;
                    }
                    else {
                        return (int) (input.peekLong(2) + target - remain);
                    }
                }
                case 0x7E -> {
                    int target = withMask ? 6 : 4;
                    if(remain < target) {
                        return target - remain;
                    }
                    else {
                        return input.peekUnsignedShort(2) + target - remain;
                    }
                }
                default -> {
                    int target = withMask ? 4 : 2;
                    if(remain < target) {
                        return target - remain;
                    }
                    else {return lack + target - remain;}
                }
            }
        }
    }

    @Override
    public int prefix(ByteBuf input)
    {
        mFrameHeader = input.get();
        int code = input.get();
        boolean withMask = (code & 0x80) > 0;
        int payloadLength = code & 0x7F;
        if(payloadLength > 0x7E) {
            payloadLength = (int) input.getLong();
        }
        else if(payloadLength > 0x7D) {
            payloadLength = input.getUnsignedShort();
        }
        if(withMask) {
            mMask = new byte[4];
            input.get(mMask);
        }
        return payloadLength;
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain];
            input.get(mPayload);
            doMask();
        }
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.put(mFrameHeader |= frame_fin_no_more);
        int attr = 0;
        output.markWriter();
        if(mPayload != null) {
            if(mPayload.length > 0xFFFF) {
                attr |= 0x7F;
                output.putLong(mPayload.length);
            }
            else if(mPayload.length > 0x7D) {
                attr |= 0x7E;
                output.putShort((short) mPayload.length);
            }
            else {
                attr |= mPayload.length;
            }
        }
        if(mMask != null) {
            output.put(mMask);
            attr |= 0x80;
            doMask();
        }
        output.put(attr, output.writerMark());
        if(mPayload != null) {
            output.put(mPayload);
        }
        return output;
    }

    @Override
    public IoSerial subContent()
    {
        return mSubContent;
    }

    @Override
    public WsFrame withSub(IoSerial sub)
    {
        //IoSerial payload ≤ 256MB不会超过payload, 对多fragment场景支持欠缺
        ByteBuf encoded = sub.encode();
        if(encoded != null && encoded.capacity() > 0) {mPayload = encoded.array();}
        mSubContent = sub;
        return this;
    }

    @Override
    public WsFrame withSub(byte[] sub)
    {
        mPayload = sub == null || sub.length > 0 ? sub : null;
        return this;
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        Objects.requireNonNull(factory)
               .create(subEncoded());
    }

    @Override
    public Level getLevel()
    {
        return Level.ALMOST_ONCE;
    }

    public static int peekSubSerial(ByteBuf buffer)
    {
        return Objects.requireNonNull(buffer)
                      .peek(0) & 0x0F;
    }

}
