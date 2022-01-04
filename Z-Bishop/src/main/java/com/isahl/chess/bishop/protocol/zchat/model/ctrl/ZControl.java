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

    protected byte mAttr;

    private ISession mSession;
    private ZContext mContext;
    private long     mSequence;

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
    public ZControl wrap(ZContext context)
    {
        mContext = context;
        return this;
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

    @Override
    public ZControl withSub(IoSerial subContent)
    {
        Objects.requireNonNull(subContent);
        mAttr |= attribute_sub_content;
        mSubContent = subContent;
        ByteBuf subEncoded = mSubContent.encode();
        if(subEncoded.capacity() > 0) {mPayload = subEncoded.array();}
        return this;
    }

    @Override
    public ZControl withSub(byte[] sub)
    {
        mPayload = sub == null || sub.length > 0 ? sub : null;
        return this;
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
            throw new ZException("crc check failed! = %#x", serial());
        }
    }

    @Override
    public int length()
    {
        return 2 + // attr,cmd-id
               (isWithId() ? 8 : 0) + //msgId
               super.length() + // payload-length
               4; //crc
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
    public ByteBuf encode(ZContext ctx)
    {
        ByteBuf output = ByteBuf.allocate(this.sizeOf());
        suffix(output, ctx);
        if(mPayload != null) {output.put(mPayload);}
        addCrc(output);
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
        wrap(session.getContext(ZContext.class));
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

    @Override
    public boolean isMapping()
    {
        return true;
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
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public IoSerial subContent()
    {
        return mSubContent;
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        mSubContent = factory.create(subEncoded());
    }
}
