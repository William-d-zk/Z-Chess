/*
 *  MIT License
 *
 * Copyright (c) 2016-2024.  Z-Chess
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of
 *   this software and associated documentation files (the "Software"), to deal in
 *   the Software without restriction, including without limitation the rights to
 *   use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *   the Software, and to permit persons to whom the Software is furnished to do so,
 *   subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.isahl.chess.pawn.endpoint.device.db.central.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.io.Serial;
import java.nio.charset.StandardCharsets;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_INSERT;

@Entity(name = "zc_rs_message-delivery_status")
public class MsgDeliveryStatus
        extends AuditModel
{

    @Serial
    private static final long    serialVersionUID = -1273138382634702442L;
    @Transient
    private              String  mStatus;
    @Transient
    private              boolean mEnable;
    @Transient
    private              String  mFlag;

    @Id
    @JsonIgnore
    public long getId()
    {
        return pKey;
    }

    public void setId(long id) {pKey = id;}

    public MsgDeliveryStatus()
    {
        super(OP_INSERT, Strategy.RETAIN);
    }

    public MsgDeliveryStatus(ByteBuf input)
    {
        super(input);
    }

    public void setStatus(String status) {mStatus = status;}

    @Column(name = "notice")
    public String getStatus() {return mStatus;}

    @Column(name = "enable")
    public boolean isEnable() {return mEnable;}

    public void setEnable(boolean enable) {mEnable = enable;}

    @Column(name = "flag")
    public String getFlag() {return mFlag;}

    public void setFlag(String flag) {mFlag = flag;}

    @Transient
    public boolean isStart() {return mFlag.equals("start");}

    @Transient
    public boolean isEnd() {return mFlag.equals("end");}

    @Transient
    public boolean isDoing() {return mFlag.equals("doing");}

    @Override
    public int length()
    {
        return super.length() + // content.length +  id.length
               1 +// enable.length
               vSizeOf(mFlag.getBytes(StandardCharsets.UTF_8).length) + // flag.length
               vSizeOf(mStatus.getBytes(StandardCharsets.UTF_8).length); // topic.length
    }

    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mEnable = input.get() == 1;
        remain -= 1;
        int tl = input.vLength();
        mFlag = input.readUTF(tl);
        remain -= vSizeOf(tl);
        tl = input.vLength();
        mStatus = input.readUTF(tl);
        remain -= vSizeOf(tl);
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .put((byte) (mEnable ? 1 : 0))
                    .putUTF(mFlag)
                    .putUTF(mStatus);
    }
}
