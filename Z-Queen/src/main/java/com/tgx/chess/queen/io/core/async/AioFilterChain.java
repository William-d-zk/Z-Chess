/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.queen.io.core.async;

import static com.tgx.chess.queen.io.core.inf.IContext.DECODE_HANDSHAKE;
import static com.tgx.chess.queen.io.core.inf.IContext.ENCODE_HANDSHAKE;

import java.util.Objects;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IFilter;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IFrame;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public abstract class AioFilterChain<C extends IContext<C>,
                                     O extends IProtocol,
                                     I extends IProtocol>
        implements
        IFilterChain<C>,
        IFilter<C,
                O,
                I>
{

    protected final Logger _Logger;

    private final String    _Name;
    private IFilterChain<C> next;
    private IFilterChain<C> previous;

    private int mIdempotent = 0x80000000;

    protected AioFilterChain(String name)
    {
        _Name = name;
        _Logger = Logger.getLogger("io.queen.chain." + _Name);
    }

    @Override
    public int getIdempotentBit()
    {
        return mIdempotent;
    }

    @Override
    public void idempotentRightShift(int previous)
    {
        if (previous == 1) { throw new IllegalArgumentException(); }
        mIdempotent = previous == 0 && mIdempotent == 0x80000000 ? 1
                                                                 : previous != 0 ? previous >>> 1
                                                                                 : mIdempotent;
    }

    @Override
    public IFilterChain<C> getPrevious()
    {
        return previous;
    }

    @Override
    public void setPrevious(IFilterChain<C> previous)
    {
        this.previous = previous;
    }

    @Override
    public IFilterChain<C> getNext()
    {
        return next;
    }

    @Override
    public void setNext(IFilterChain<C> next)
    {
        this.next = next;
    }

    @Override
    public IFilterChain<C> getChainHead()
    {
        IFilterChain<C> node = previous;
        while (node != null && node.getPrevious() != null) {
            node = node.getPrevious();
        }
        return node == null ? this
                            : node;
    }

    @Override
    public IFilterChain<C> getChainTail()
    {
        IFilterChain<C> node = next;
        while (node != null && node.getNext() != null) {
            node = node.getNext();
        }
        return node == null ? this
                            : node;
    }

    @Override
    public IFilterChain<C> linkAfter(IFilterChain<C> current)
    {
        if (current == null) { return this; }
        current.setNext(this);
        setPrevious(current);
        idempotentRightShift(current.getIdempotentBit());
        return this;
    }

    @Override
    public IFilterChain<C> linkFront(IFilterChain<C> current)
    {
        if (current == null) { return this; }
        current.setPrevious(this);
        setNext(current);
        current.idempotentRightShift(getIdempotentBit());
        return current;
    }

    @Override
    public void dispose()
    {
        IFilterChain<C> nextNext;
        IFilterChain<C> next = this.next;
        while (next != null) {
            nextNext = next.getNext();
            next.setNext(null);
            next = nextNext;
        }
    }

    protected ResultType preCommandEncode(C context, IProtocol output)
    {
        if (Objects.isNull(output) || Objects.isNull(context)) { return ResultType.ERROR; }
        return context.isOutConvert() && output.superSerial() == IProtocol.COMMAND_SERIAL ? ResultType.NEXT_STEP
                                                                                          : ResultType.IGNORE;

    }

    protected ResultType preCommandDecode(C context, IFrame input)
    {
        if (Objects.isNull(context) || Objects.isNull(input)) { return ResultType.ERROR; }
        return context.isInConvert()
               && input.superSerial() == IProtocol.FRAME_SERIAL
               && !input.isCtrl() ? ResultType.HANDLED
                                  : ResultType.IGNORE;
    }

    protected ResultType preFrameEncode(C context, IProtocol output)
    {
        if (Objects.isNull(context) || Objects.isNull(output)) { return ResultType.ERROR; }
        return context.isOutConvert() && output.superSerial() == IProtocol.FRAME_SERIAL ? ResultType.NEXT_STEP
                                                                                        : ResultType.IGNORE;
    }

    protected ResultType preFrameDecode(C context, IPacket input)
    {
        if (Objects.isNull(context) || Objects.isNull(input)) { return ResultType.ERROR; }
        return context.isInConvert() && input.superSerial() == IProtocol.FRAME_SERIAL ? ResultType.NEXT_STEP
                                                                                      : ResultType.IGNORE;
    }

    protected ResultType prePacketEncode(C context, IProtocol output)
    {
        if (Objects.isNull(context) || Objects.isNull(output)) { return ResultType.ERROR; }
        return context.isOutConvert() && output.superSerial() == IProtocol.PACKET_SERIAL ? ResultType.NEXT_STEP
                                                                                         : ResultType.IGNORE;
    }

    protected ResultType prePacketDecode(C context, IPacket input)
    {
        if (Objects.isNull(context) || Objects.isNull(input)) { return ResultType.ERROR; }
        return context.isInConvert() && input.superSerial() == IProtocol.PACKET_SERIAL ? ResultType.NEXT_STEP
                                                                                       : ResultType.IGNORE;
    }

    protected ResultType preControlEncode(C context, IProtocol output)
    {
        if (Objects.isNull(context) || Objects.isNull(output)) { return ResultType.ERROR; }
        return context.isOutConvert() && output.superSerial() == IProtocol.CONTROL_SERIAL ? ResultType.NEXT_STEP
                                                                                          : ResultType.IGNORE;
    }

    protected ResultType preControlDecode(C context, IFrame input)
    {
        if (Objects.isNull(context) || Objects.isNull(input)) { return ResultType.ERROR; }
        return context.isInConvert()
               && input.superSerial() == IProtocol.FRAME_SERIAL
               && input.isCtrl() ? ResultType.HANDLED
                                 : ResultType.IGNORE;

    }

    protected ResultType preHandShakeEncode(C context, IProtocol output)
    {
        if (Objects.isNull(context) || Objects.isNull(output)) { return ResultType.ERROR; }
        return context.needHandshake()
               && context.outState() == ENCODE_HANDSHAKE
               && output.superSerial() == IProtocol.CONTROL_SERIAL ? ResultType.NEXT_STEP
                                                                   : ResultType.IGNORE;
    }

    protected ResultType preHandShakeDecode(C context, IPacket input)
    {
        if (Objects.isNull(context) || Objects.isNull(input)) { return ResultType.ERROR; }
        return context.needHandshake()
               && context.inState() == DECODE_HANDSHAKE
               && input.superSerial() == IProtocol.PACKET_SERIAL ? ResultType.HANDLED
                                                                 : ResultType.IGNORE;

    }

    @Override
    @SuppressWarnings("unchecked")
    public IFilter<C,
                   O,
                   I> getFilter()
    {
        return this;
    }

    @Override
    public String getName()
    {
        return _Name;
    }
}
