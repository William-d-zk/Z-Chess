package com.isahl.chess.bishop.protocol.zchat.model.ctrl;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZProtocol;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.IDuplicate;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.I18nUtil;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.nio.charset.Charset;
import java.util.Objects;

public abstract class ZControl
        extends ZProtocol
        implements IControl<ZContext>,
                   IDuplicate
{
    protected final static int attribute_rsv_msg_id        = 0x80;
    protected final static int attribute_sub_content       = 0x10;
    protected final static int attribute_version_bits_left = 5;
    protected final static int attribute_version_mask      = 3 << attribute_version_bits_left;

    private final static int min_no_msg_uid_size = 1 + 1 + 4;
    private final static int g_msg_uid_size      = 8;
    private final static int min_msg_uid_size    = min_no_msg_uid_size + g_msg_uid_size;

    protected byte[] mPayload;
    protected byte   mFrameHeader, mAttr;
    protected IoSerial mSubContent;

    private ISession mSession;
    private long     mSequence;
    private ZContext mContext;

    public ZControl()
    {
        setVersion();
        withId(false);
        mFrameHeader |= ZFrame.frame_op_code_ctrl | (getLevel().getValue() << ZFrame.frame_op_code_qos_bit_left);
    }

    @Override
    public int _sub()
    {
        return mSubContent == null ? -1 : mSubContent.serial();
    }

    @Override
    public void wrap(ZContext context)
    {
        mContext = context;
    }

    protected void withId(boolean flag)
    {
        mAttr |= flag ? attribute_rsv_msg_id : 0;
    }

    protected boolean isWithId()
    {
        return (mAttr & attribute_rsv_msg_id) > 0;
    }

    protected void setVersion()
    {
        mAttr |= (ZProtocol.VERSION << attribute_version_bits_left) & attribute_version_mask;
    }

    public void withSub(IoSerial subContent)
    {
        Objects.requireNonNull(subContent);
        mAttr |= attribute_sub_content;
        mSubContent = subContent;
        mPayload = mSubContent.encode()
                              .array();
    }

    public boolean isWithSub()
    {
        return (mAttr & attribute_sub_content) > 0;
    }

    protected void addCrc(ByteBuf output)
    {
        output.putInt(CryptoUtil.crc32(output.array(), output.writerMark(), output.writerIdx() - output.writerMark()));
    }

    protected void checkCrc(ByteBuf input)
    {
        if(CryptoUtil.crc32(input.array(), input.readerMark(), input.readerIdx() - input.readerMark()) !=
           input.getInt())
        {
            throw new ZException("crc check failed! = %#x", seekSerial(input, input.readerMark()));
        }
    }

    @Override
    public int length()
    {
        return 2 + (mPayload == null ? 0 : mPayload.length) + 4;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        input.markReader();
        mAttr = input.get();
        int cmd = input.getUnsigned();
        if(cmd != serial()) {
            throw new ZException("input.cmd[%d]≠self.serial[%d]", cmd, serial());
        }
        if(isWithId()) {
            throw new ZException("z-chat-control with msg-id?!");
        }
        return input.readableBytes();
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 4) {
            mPayload = new byte[remain - 4];
            input.get(mPayload);
        }
        checkCrc(input);
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output.markWriter();
        output.put(mAttr);
        output.put(serial());
        return output;
    }

    @Override
    public ByteBuf encode()
    {
        ByteBuf output = ByteBuf.allocate(this.sizeOf());
        suffix(output);
        if(mPayload != null) {output.put(mPayload);}
        addCrc(output);
        return output;
    }

    @Override
    public ZControl with(ISession session)
    {
        mSession = session;
        return this;
    }

    @Override
    public ISession session()
    {
        return mSession;
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
        return (mFrameHeader & ZFrame.frame_op_code_ctrl) > 0;
    }

    public ZControl duplicate()
    {
        mFrameHeader |= ZFrame.frame_op_code_duplicate;
        return this;
    }

    public Charset getCharset()
    {
        return I18nUtil.getCharset(mAttr);
    }

    @Override
    public ZContext context()
    {
        return mContext;
    }

    @Override
    public long getSequence()
    {
        return mSequence;
    }

    @Override
    public void setSequence(long sequence)
    {
        mSequence = mSequence < 0 ? sequence : mSequence;
    }

    @Override
    public ByteBuf payload()
    {
        return mPayload == null ? null : ByteBuf.wrap(mPayload);
    }

    @Override
    public IoSerial subContent()
    {
        return mSubContent;
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        ByteBuf payload = payload();
        mSubContent = factory.create(payload);
        mSubContent.decode(payload);
    }

    public static int seekSerial(ByteBuf buffer, int position)
    {
        int attr = buffer.get(position++);
        return buffer.getUnsigned(position);
    }
}