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
package com.isahl.chess.queen.events.model;

import com.isahl.chess.king.base.disruptor.features.event.IEvent;
import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.lmax.disruptor.EventFactory;

import java.util.List;

import static com.isahl.chess.king.base.features.IError.Type.NO_ERROR;

/**
 * @author William.d.zk
 */
public class QEvent
        implements IEvent
{
    public static final EventFactory<QEvent> EVENT_FACTORY = new QEventFactory();

    private IError.Type              mErrType = NO_ERROR;
    private OperateType              mType    = OperateType.NULL;
    private IPair                    mComponent;
    private IBinaryOperator<?, ?, ?> mBinaryOperator;
    private IoSerial                 mContent;
    private Throwable                mThrowable;
    private IOperator<?, ?>          mOperator;
    private List<ITriple>            mResultList;

    @Override
    public String toString()
    {
        return String.format(
                "\nerror: %s\n\ttype:%s\n\tresults:%s\n\tcontent:%s\n\toperator:%s\n\tcomponent:%s\n\tbinary_operator:%s\n\t",
                mErrType,
                mType,
                mResultList,
                mContent,
                mOperator,
                mComponent,
                mBinaryOperator);
    }

    @Override
    public void reset()
    {
        mType = OperateType.NULL;
        mErrType = NO_ERROR;
        mBinaryOperator = null;
        mComponent = null;
        mContent = null;
        mOperator = null;
        mResultList = null;
    }

    public void ignore()
    {
        mType = OperateType.IGNORE;
        mErrType = NO_ERROR;
        mComponent = null;
        mBinaryOperator = null;
        mContent = null;
        mOperator = null;
        mResultList = null;
    }

    public void transfer(QEvent dest)
    {
        dest.mType = mType;
        dest.mErrType = mErrType;
        dest.mBinaryOperator = mBinaryOperator;
        dest.mComponent = mComponent != null ? (IPair) mComponent.duplicate() : null;
        dest.mContent = mContent;
        dest.mOperator = mOperator;
        dest.mResultList = mResultList;
    }

    @Override
    public OperateType getEventType()
    {
        return mType;
    }

    @Override
    public IError.Type getErrorType()
    {
        return mErrType;
    }

    @Override
    public <V, A, R> void produce(OperateType t, IPair component, IBinaryOperator<V, A, R> binaryOperator)
    {
        mErrType = NO_ERROR;
        mType = t;
        mComponent = component;
        mBinaryOperator = binaryOperator;
        mResultList = null;
    }

    @Override
    public <V, R> void produce(OperateType t, IoSerial content, IOperator<V, R> operator)
    {
        mErrType = NO_ERROR;
        mType = t;
        mContent = content;
        mOperator = operator;
        mResultList = null;
    }

    @Override
    public void produce(OperateType t, List<ITriple> triples)
    {
        mErrType = NO_ERROR;
        mType = t;
        mComponent = null;
        mBinaryOperator = null;
        mResultList = triples;
    }

    @Override
    public <E, H, R> void error(IError.Type t, IPair component, IBinaryOperator<E, H, R> binaryOperator)
    {
        mType = OperateType.NULL;
        mErrType = t;
        mComponent = component;
        mBinaryOperator = binaryOperator;
        mResultList = null;
    }

    @Override
    public <R> void error(IError.Type t, Throwable throwable, IOperator<Throwable, R> operator)
    {
        mType = OperateType.NULL;
        mErrType = t;
        mThrowable = throwable;
        mOperator = operator;
        mResultList = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, A, R> IBinaryOperator<V, A, R> getEventBinaryOp()
    {
        return (IBinaryOperator<V, A, R>) mBinaryOperator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> IOperator<T, R> getEventOp()
    {
        return (IOperator<T, R>) mOperator;
    }

    @Override
    public IPair getComponent()
    {
        return mComponent;
    }

    @Override
    public IoSerial getContent()
    {
        return mContent;
    }

    @Override
    public List<ITriple> getResultList()
    {
        return mResultList;
    }

    private static class QEventFactory
            implements EventFactory<QEvent>
    {
        @Override
        public QEvent newInstance()
        {
            return new QEvent();
        }
    }

}
