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
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.nio.ByteBuffer;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_FRAME_SERIAL)
public class WsFrame
        implements IReset,
                   IFrame
{
    public final static byte    frame_op_code_ctrl_close                 = 0x08;
    public final static byte    frame_op_code_ctrl_ping                  = 0x09;
    public final static byte    frame_op_code_ctrl_pong                  = 0x0A;
    public final static byte    frame_op_code_ctrl_handshake             = 0x00;
    public final static byte    frame_op_code_mask                       = 0x0F;
    public final static byte    frame_fin_more                           = 0x00;
    public final static byte    frame_fin_no_more                        = (byte) 0x80;
    public final static byte    frame_rsv_1_mask                         = 0x40;
    public final static byte    frame_rsv_2_mask                         = 0x20;
    public final static byte    frame_rsv_3_mask                         = 0x10;
    public final static byte    frame_op_code_no_ctrl_count              = 0x00;
    public final static byte    frame_op_code_no_ctrl_txt                = 0x01;
    public final static byte    frame_op_code_no_ctrl_bin                = 0x02;
    public final static byte    frame_op_code_no_ctrl_json               = 0x03;
    public final static byte    frame_op_code_no_ctrl_zq                 = 0x04;
    public final static byte    frame_max_header_size                    = 14;
    public final static int     frame_payload_length_7_no_mask_position  = 1;
    public final static int     frame_payload_length_7_mask_position     = 5;
    public final static int     frame_payload_length_16_no_mask_position = 3;
    public final static int     frame_payload_length_16_mask_position    = 7;
    public final static int     frame_payload_length_63_no_mask_position = 9;
    public final static int     frame_payload_length_63_mask_position    = 13;
    public              byte    frame_op_code;
    public              boolean frame_fin;
    public              boolean frame_fragment;
    private             byte[]  mMask;
    private             byte[]  mPayload;
    private             long    mPayloadLength                           = -1;
    // mask | first payload_length
    private             byte    mMaskCode;
    private             int     mMaskLength;
    private             int     mSub;

    public WsFrame()
    {
        setTypeBin();
    }

    public static byte getFragmentFrame()
    {
        return frame_fin_more;
    }

    public static byte getFragmentEndFrame()
    {
        return frame_fin_no_more;
    }

    public static byte getFirstFragmentFrame(byte frame_op_code)
    {
        return frame_op_code;
    }

    public static byte getFrame(byte frame_op_code)
    {
        return (byte) (frame_fin_no_more | frame_op_code);
    }

    public static byte getOpCode(byte value)
    {
        return (byte) (frame_op_code_mask & value);
    }

    public static boolean isFrameFin(byte value)
    {
        return (value & 0x80) != 0;
    }

    public void doMask()
    {
        if(mMaskLength > 0 && mPayloadLength > 0) {
            for(int i = 0; i < mPayloadLength; i++) {mPayload[i] ^= mMask[i & 3];}
        }
    }

    @Override
    public ByteBuffer payload()
    {
        return mPayload == null ? null : ByteBuffer.wrap(mPayload);
    }

    @Override
    public void put(byte[] payload)
    {
        mPayload = payload;
        mPayloadLength = payload == null ? 0 : payload.length;
    }

    public byte[] getMask()
    {
        return mMask;
    }

    public void setMask(byte[] mask)
    {
        if(mask != null) {
            mMask = mask;
            mMaskLength = mask.length;
        }
    }

    public long getPayloadLength()
    {
        return mPayloadLength;
    }

    public void setPayloadLength(long length)
    {
        mPayloadLength = length;
    }

    public int getMaskLength()
    {
        return mMaskLength;
    }

    public byte[] getPayloadLengthArray()
    {
        if(mPayloadLength <= 0) {return new byte[]{ (byte) (mMask == null ? 0 : 0x80) };}
        int t_size = mPayloadLength > 0xFFFF ? 9 : mPayloadLength > 0x7D ? 3 : 1;
        byte[] x = new byte[t_size];
        if(mPayloadLength > 0xFFFF) {
            x[0] = 0x7F;
            IoUtil.writeLong(mPayloadLength, x, 1);
        }
        else if(mPayloadLength > 0x7D) {
            x[0] = 0x7E;
            IoUtil.writeShort((int) mPayloadLength, x, 1);
        }
        else {x[0] = (byte) mPayloadLength;}
        if(mMask != null) {mMaskCode = x[0] |= 0x80;}
        return x;
    }

    public WsFrame setTypeTxt()
    {
        frame_op_code = frame_op_code_no_ctrl_txt;
        return this;
    }

    public WsFrame setTypeBin()
    {
        frame_op_code = frame_op_code_no_ctrl_bin;
        return this;
    }

    public WsFrame setTypeZQ()
    {
        frame_op_code = frame_op_code_no_ctrl_zq;
        return this;
    }

    public WsFrame setTypeJson()
    {
        frame_op_code = frame_op_code_no_ctrl_json;
        return this;
    }

    @Override
    public int length()
    {
        return 1 + mMaskLength + (int) mPayloadLength + (mPayloadLength > 0xFFFF ? 9 : mPayloadLength > 0x7D ? 3 : 1);

    }

    @Override
    public int lack(int position)
    {
        int result = (mMaskCode & 0x80) != 0 ? 4 : 0;
        return switch(mMaskCode & 0x7F) {
            case 0x7F -> 9 + result;
            case 0x7E -> 3 + result;
            default -> 1 + result;
        };
    }

    public void setMaskCode(byte b)
    {
        mMaskCode = b;
    }

    public byte getLengthCode()
    {
        return (byte) (mMaskCode & 0x7F);
    }

    public byte getFrameFin()
    {
        if(frame_fragment) {return frame_fin ? getFragmentEndFrame() : getFragmentFrame();}
        else {return getFrame(frame_op_code);}
    }

    public boolean checkRSV(byte value)
    {
        return (value & frame_rsv_1_mask) == 0 && (value & frame_rsv_2_mask) == 0 && (value & frame_rsv_3_mask) == 0;
    }

    @Override
    public void put(byte frame_ctrl_code)
    {
        frame_op_code = frame_ctrl_code;
    }

    @Override
    public byte ctrl()
    {
        return (byte) (frame_op_code & 0x0F);
    }

    @Override
    public boolean isCtrl()
    {
        return (frame_op_code & 0x08) != 0;
    }

    @Override
    public void reset()
    {
        mMask = null;
        mPayload = null;
        mPayloadLength = -1;
        mMaskLength = -1;
        mMaskCode = 0;
        frame_op_code = 0;
        frame_fragment = false;
        frame_fin = false;
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        byte attr = input.get();
        frame_op_code = getOpCode(attr);
        frame_fin = isFrameFin(attr);
        mMaskCode = input.get();
        int p = lack(input.position());
        switch(p) {
            case WsFrame.frame_payload_length_7_no_mask_position:
                setPayloadLength(mMaskCode & 0x7F);
                setMask(null);
                break;
            case WsFrame.frame_payload_length_16_no_mask_position:
                setPayloadLength(input.getShort() & 0xFFFF);
                setMask(null);
                break;
            case WsFrame.frame_payload_length_7_mask_position:
                setPayloadLength(mMaskCode & 0x7F);
                byte[] mask = new byte[4];
                input.get(mask);
                setMask(mask);
                break;
            case WsFrame.frame_payload_length_16_mask_position:
                setPayloadLength(input.getShort() & 0xFFFF);
                mask = new byte[4];
                input.get(mask);
                setMask(mask);
                break;
            case WsFrame.frame_payload_length_63_no_mask_position:
                setPayloadLength(input.getLong());
                setMask(null);
                break;
            case WsFrame.frame_payload_length_63_mask_position:
                setPayloadLength(input.getLong());
                mask = new byte[4];
                input.get(mask);
                setMask(mask);
                break;
        }
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        output.put(getFrameFin());
        output.put(getPayloadLengthArray());
        if(getMaskLength() > 0) {
            output.put(getMask());
        }
        if(getPayloadLength() > 0) {
            doMask();
            output.put(mPayload);
        }
    }

    @Override
    public int _sub()
    {
        return mSub;
    }
}
