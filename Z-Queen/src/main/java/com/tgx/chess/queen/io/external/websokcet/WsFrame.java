/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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
package com.tgx.chess.queen.io.external.websokcet;

import com.tgx.chess.king.base.inf.IReset;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.inf.IFrame;

/**
 * @author William.d.zk
 */
public class WsFrame
        implements
        IReset,
        IFrame
{
    private final static int WEB_SOCKET_FRAME = FRAME_SERIAL + 1;

    @Override
    public int getSerial() {
        return WEB_SOCKET_FRAME;
    }

    public final static byte frame_op_code_ctrl_close                 = 0x08;
    public final static byte frame_op_code_ctrl_ping                  = 0x09;
    public final static byte frame_op_code_ctrl_pong                  = 0x0A;
    public final static byte frame_op_code_ctrl_cluster               = 0x0B;
    public final static byte frame_op_code_ctrl_handshake             = 0x00;
    public final static byte frame_op_code_mask                       = 0x0F;
    public final static byte frame_fin_more                           = 0x00;
    public final static byte frame_fin_no_more                        = (byte) 0x80;
    public final static byte frame_rsv_1_mask                         = 0x40;
    public final static byte frame_rsv_2_mask                         = 0x20;
    public final static byte frame_rsv_3_mask                         = 0x10;
    public final static byte frame_op_code_no_ctrl_cont               = 0x00;
    public final static byte frame_op_code_no_ctrl_txt                = 0x01;
    public final static byte frame_op_code_no_ctrl_bin                = 0x02;
    public final static byte frame_op_code_no_ctrl_json               = 0x03;
    public final static byte frame_op_code_no_ctrl_zq                 = 0x04;
    public final static byte frame_max_header_size                    = 14;
    public final static int  frame_payload_length_7_no_mask_position  = 1;
    public final static int  frame_payload_length_7_mask_position     = 5;
    public final static int  frame_payload_length_16_no_mask_position = 3;
    public final static int  frame_payload_length_16_mask_position    = 7;
    public final static int  frame_payload_length_63_no_mask_position = 9;
    public final static int  frame_payload_length_63_mask_position    = 13;
    public byte              frame_op_code;
    public boolean           frame_fin;
    public boolean           frame_fragment;
    private byte[]           mMask;
    private byte[]           mPayload;
    private long             mPayloadLength                           = -1;
    // mask | first payload_length
    private byte             mPayload_Mask;
    private int              mMaskLength                              = -1;

    public WsFrame() {
        setTypeBin();
    }

    public static byte getFragmentFrame() {
        return frame_fin_more;
    }

    public static byte getFragmentEndFrame() {
        return frame_fin_no_more;
    }

    public static byte getFirstFragmentFrame(byte frame_op_code) {
        return frame_op_code;
    }

    public static byte getFrame(byte frame_op_code) {
        return (byte) (frame_fin_no_more | frame_op_code);
    }

    public static byte getOpCode(byte value) {
        return (byte) (frame_op_code_mask & value);
    }

    public static boolean isFrameFin(byte value) {
        return (value & 0x80) != 0;
    }

    public void doMask() {
        if (mMaskLength > 0 && mPayloadLength > 0) {
            for (int i = 0; i < mPayloadLength; i++)
                mPayload[i] ^= mMask[i & 3];
        }
    }

    public byte[] getPayload() {
        return mPayload;
    }

    public void setPayload(byte[] payload) {
        mPayload = payload;
        mPayloadLength = payload == null ? 0 : payload.length;

    }

    public byte[] getMask() {
        return mMask;
    }

    public void setMask(byte[] mask) {
        mMask = mask;
        mMaskLength = mask == null ? 0 : mask.length;
    }

    public long getPayloadLength() {
        return mPayloadLength;
    }

    public void setPayloadLength(long length) {
        mPayloadLength = length;
    }

    public int getMaskLength() {
        return mMaskLength;
    }

    public byte[] getPayloadLengthArray() {
        if (mPayloadLength <= 0) return new byte[] { (byte) (mMask == null ? 0 : 0x80) };
        int t_size = mPayloadLength > 0xFFFF ? 9 : mPayloadLength > 0x7D ? 3 : 1;
        byte[] x = new byte[t_size];
        if (mPayloadLength > 0xFFFF) {
            x[0] = 0x7F;
            IoUtil.writeLong(mPayloadLength, x, 1);
        }
        else if (mPayloadLength > 0x7D) {
            x[0] = 0x7E;
            IoUtil.writeShort((int) mPayloadLength, x, 1);
        }
        else x[0] = (byte) mPayloadLength;
        if (mMask != null) x[0] |= 0x80;
        return x;
    }

    public WsFrame setTypeTxt() {
        frame_op_code = frame_op_code_no_ctrl_txt;
        return this;
    }

    public WsFrame setTypeBin() {
        frame_op_code = frame_op_code_no_ctrl_bin;
        return this;
    }

    public WsFrame setTypeZQ() {
        frame_op_code = frame_op_code_no_ctrl_zq;
        return this;
    }

    public WsFrame setTypeJson() {
        frame_op_code = frame_op_code_no_ctrl_json;
        return this;
    }

    @Override
    public int dataLength() {
        return 1 + mMaskLength + (int) mPayloadLength + (mPayloadLength > 0xFFFF ? 9 : mPayloadLength > 0x7D ? 3 : 1);

    }

    public int payloadLengthLack() {
        int result = (mPayload_Mask & 0x80) != 0 ? 4 : 0;
        switch (mPayload_Mask & 0x7F) {
            case 0x7F:
                return 9 + result;
            case 0x7E:
                return 3 + result;
            default:
                return 1 + result;
        }
    }

    public void setMaskCode(byte b) {
        mPayload_Mask = b;
    }

    public byte getLengthCode() {
        return (byte) (mPayload_Mask & 0x7F);
    }

    public byte getFrameFin() {
        if (frame_fragment) return frame_fin ? getFragmentEndFrame() : getFragmentFrame();
        else return getFrame(frame_op_code);
    }

    public boolean checkRSV(byte value) {
        return (value & frame_rsv_1_mask) == 0 && (value & frame_rsv_2_mask) == 0 && (value & frame_rsv_2_mask) == 0;
    }

    @Override
    public void setCtrl(byte frame_ctrl_code) {
        frame_op_code = frame_ctrl_code;
    }

    @Override
    public boolean isNoCtrl() {
        return (frame_op_code & 0x08) == 0;
    }

    @Override
    public boolean isCtrl() {
        return (frame_op_code & 0x08) != 0;
    }

    public boolean isCtrlClose() {
        return (frame_op_code & 0x0F) == frame_op_code_ctrl_close;
    }

    public boolean isCtrlPing() {
        return (frame_op_code & 0x0F) == frame_op_code_ctrl_ping;
    }

    public boolean isCtrlPong() {
        return (frame_op_code & 0x0F) == frame_op_code_ctrl_pong;
    }

    public boolean isCtrlHandShake() {
        return (frame_op_code & 0x0F) == frame_op_code_ctrl_handshake;
    }

    public boolean isCtrlCluster() {
        return (frame_op_code & 0x0F) == frame_op_code_ctrl_cluster;
    }

    @Override
    public void reset() {
        mMask = null;
        mPayload = null;
        mPayloadLength = -1;
        mMaskLength = -1;
        mPayload_Mask = 0;
        frame_op_code = 0;
        frame_fragment = false;
        frame_fin = false;
    }

    @Override
    public int decodec(byte[] data, int pos) {
        byte attr = data[pos++];
        frame_op_code = getOpCode(attr);
        frame_fin = isFrameFin(attr);
        mPayload_Mask = data[pos++];
        int p = payloadLengthLack();
        switch (p) {
            case WsFrame.frame_payload_length_7_no_mask_position:
                setPayloadLength(mPayload_Mask & 0x7F);
                setMask(null);
                break;
            case WsFrame.frame_payload_length_16_no_mask_position:
                setPayloadLength(IoUtil.readUnsignedShort(data, pos));
                setMask(null);
                pos += 2;
                break;
            case WsFrame.frame_payload_length_7_mask_position:
                setPayloadLength(mPayload_Mask & 0x7F);
                byte[] mask = new byte[4];
                pos = IoUtil.read(data, pos, mask);
                setMask(mask);
                break;
            case WsFrame.frame_payload_length_16_mask_position:
                setPayloadLength(IoUtil.readUnsignedShort(data, pos));
                pos += 2;
                mask = new byte[4];
                pos = IoUtil.read(data, pos, mask);
                setMask(mask);
                break;
            case WsFrame.frame_payload_length_63_no_mask_position:
                setPayloadLength(IoUtil.readLong(data, pos));
                pos += 8;
                setMask(null);
                break;
            case WsFrame.frame_payload_length_63_mask_position:
                setPayloadLength(IoUtil.readLong(data, pos));
                pos += 8;
                mask = new byte[4];
                pos = IoUtil.read(data, pos, mask);
                setMask(mask);
                break;
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos) {
        pos += IoUtil.writeByte(getFrameFin(), data, pos);
        pos += IoUtil.write(getPayloadLengthArray(), data, pos);
        if (getMaskLength() > 0) pos += IoUtil.write(getMask(), data, pos);
        if (getPayloadLength() > 0) {
            doMask();
            pos += IoUtil.write(getPayload(), data, pos);
        }
        return pos;
    }

}
