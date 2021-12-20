package com.isahl.chess.bishop.protocol.zchat.model.base;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.IDuplicate;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.util.Objects;

@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_FRAME_SERIAL)
public class ZFrame
        extends ZProtocol
        implements IFrame,
                   IDuplicate
{
    public final static  byte frame_op_code_transaction   = 0x20;
    public final static  byte frame_op_code_duplicate     = 0x10;
    public final static  byte frame_op_code_qos_1         = 0x04;
    public final static  byte frame_op_code_qos_2         = 0x08;
    public final static  byte frame_op_code_qos_3         = 0x0C;
    public final static  byte frame_op_code_qos_mask      = 0x0C;
    public final static  byte frame_op_code_qos_bit_left  = 2;
    public final static  byte frame_op_code_sequence_1    = 0x01;
    public final static  byte frame_op_code_sequence_2    = 0x02;
    public final static  byte frame_op_code_sequence_3    = 0x03;
    public final static  byte frame_op_code_sequence_mask = 0x03;
    public final static  byte frame_op_code_ctrl          = 0x40;
    private final static int  frame_op_code_fin_more      = 0x80;

    public final static int frame_payload_length_7_no_transaction_position  = 1;
    public final static int frame_payload_length_14_no_transaction_position = 2;
    public final static int frame_payload_length_21_no_transaction_position = 3;
    public final static int frame_payload_length_28_no_transaction_position = 4;
    public final static int frame_payload_length_7_transaction_position     = 9;
    public final static int frame_payload_length_14_transaction_position    = 10;
    public final static int frame_payload_length_21_transaction_position    = 11;
    public final static int frame_payload_length_28_transaction_position    = 12;

    private long mTransaction;

    @Override
    public int length()
    {
        return 1 + (isTransactional() ? 8 : 0) + ByteBuf.vSizeOf(mPayload == null ? 0 : mPayload.length);
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
    public int lack(ByteBuf input)
    {
        int remain = input.readableBytes();
        if(remain == 0) {
            return 1;
        }
        else {
            header(input.get(0));
            if(isTransactional()) {
                if(remain < 9) {
                    return 9 - remain;
                }
                try {
                    int vLength = input.vLength(9);
                    return 9 + ByteBuf.vSizeOf(vLength) - remain;
                }
                catch(ZException e) {
                    return 1;
                }
            }
            else {
                try {
                    int vLength = input.vLength(1);
                    return 1 + ByteBuf.vSizeOf(vLength) - remain;
                }
                catch(ZException e) {
                    return 1;
                }
            }
        }
    }

    @Override
    public Level getLevel()
    {
        return Level.valueOf((mFrameHeader & frame_op_code_qos_mask) >> frame_op_code_qos_bit_left);
    }

    public void setLevel(Level level)
    {
        mFrameHeader |= level.getValue() << frame_op_code_qos_bit_left;
    }

    @Override
    public boolean isCtrl()
    {
        return (mFrameHeader & frame_op_code_ctrl) > 0;
    }

    public boolean isFinished()
    {
        return (mFrameHeader & frame_op_code_fin_more) > 0;
    }

    @Override
    public boolean isDuplicate()
    {
        return (mFrameHeader & frame_op_code_duplicate) > 0;
    }

    public boolean isTransactional()
    {
        return (mFrameHeader & frame_op_code_transaction) > 0;
    }

    public void setTransactional(long transaction)
    {
        mFrameHeader |= frame_op_code_transaction;
        mTransaction = transaction;
    }

    @Override
    public ZFrame duplicate()
    {
        mFrameHeader |= frame_op_code_duplicate;
        return this;
    }

    public ZFrame withMore()
    {
        mFrameHeader |= frame_op_code_fin_more;
        return this;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        mFrameHeader = input.get();
        if(isTransactional()) {
            mTransaction = input.getLong();
        }
        return input.vLength();
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain];
            input.get(mPayload);
        }
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.put(mFrameHeader);
        if(isTransactional()) {
            output.putLong(mTransaction);
        }
        if(mPayload != null) {
            output.vPutLength(mPayload.length);
            output.put(mPayload);
        }
        else {output.put(0);}
        return output;
    }

    public int getSeq()
    {
        return mFrameHeader & frame_op_code_sequence_mask;
    }

    public ZFrame setSeq(int seq)
    {
        mFrameHeader |= seq & frame_op_code_sequence_mask;
        return this;
    }

    @Override
    public ZFrame copy()
    {
        ZFrame frame = new ZFrame();
        frame.mFrameHeader = mFrameHeader;
        frame.mTransaction = mTransaction;
        return frame;
    }

    public ZFrame[] put(byte... payload)
    {
        if(payload != null) {
            if(payload.length > ByteBuf.maxLength()) {
                if(payload.length > ByteBuf.maxLength() << 2) {
                    throw new ZException("payload size [%d] out of size(1GB)", payload.length);
                }
                setTransactional(System.currentTimeMillis());
                int size = payload.length / ByteBuf.maxLength();
                ZFrame[] multiFrames = new ZFrame[size];
                multiFrames[0] = this;
                mPayload = new byte[ByteBuf.maxLength()];
                IoUtil.write(payload, 0, mPayload, 0, mPayload.length);
                withMore();
                for(int i = 1, j = ByteBuf.maxLength(); i < size; i++) {
                    multiFrames[i] = copy().setSeq(i);
                    multiFrames[i].mPayload = new byte[Math.min(ByteBuf.maxLength(), payload.length - j)];
                    IoUtil.write(payload, j, multiFrames[i].mPayload, 0, multiFrames[i].mPayload.length);
                    j += multiFrames[i].mPayload.length;
                    if(multiFrames[i].mPayload.length == ByteBuf.maxLength() && i < 3) {
                        multiFrames[i].withMore();
                    }
                }
                return multiFrames;
            }
            else {mPayload = payload;}
        }
        return null;
    }

    @Override
    public IoSerial subContent()
    {
        return mSubContent;
    }

    @Override
    public ZFrame withSub(IoSerial sub)
    {
        mSubContent = sub;
        mPayload = sub.encode()
                      .array();
        return this;
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        mSubContent = Objects.requireNonNull(factory)
                             .create(payload());
    }

    @Override
    public ByteBuf payload()
    {
        return mPayload == null ? null : ByteBuf.wrap(mPayload);
    }

    public static int seekSubSerial(ByteBuf buffer)
    {
        Objects.requireNonNull(buffer);
        buffer.markReader();
        byte attr = buffer.get();
        int serial = buffer.getUnsigned();
        buffer.resetReader();
        return serial;
    }
}
