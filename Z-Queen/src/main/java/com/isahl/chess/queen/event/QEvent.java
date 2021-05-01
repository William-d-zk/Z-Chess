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
package com.isahl.chess.queen.event;

import static com.isahl.chess.king.base.inf.IError.Type.NO_ERROR;

import java.util.List;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.event.inf.IEvent;
import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.inf.IError;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.king.base.inf.ITriple;
import com.lmax.disruptor.EventFactory;

/**
 * @author William.d.zk
 */
public class QEvent
        implements
        IReset,
        IEvent
{
    public static final EventFactory<QEvent> EVENT_FACTORY = new QEventFactory();
    private IError.Type                      mErrType      = NO_ERROR;
    private OperatorType mType         = OperatorType.NULL;
    private IPair                            mContent;
    private IOperator<?,
                      ?,
                      ?>                     mOperator;
    private List<ITriple>                    mContentList;

    @Override
    public String toString()
    {
        return String.format("\nerror: %s\ntype:%s\noperator:%s\ncontent_list:%s\ncontent:%s",
                             mErrType,
                             mType,
                             mOperator,
                             mContentList,
                             mContent);
    }

    @Override
    public void reset()
    {
        mType = OperatorType.NULL;
        mErrType = NO_ERROR;
        mOperator = null;
        mContent = null;
        mContentList = null;
    }

    public void ignore()
    {
        mType = OperatorType.IGNORE;
        mErrType = NO_ERROR;
        mContent = null;
        mOperator = null;
        mContentList = null;
    }

    public void transfer(QEvent dest)
    {
        dest.mType = mType;
        dest.mErrType = mErrType;
        dest.mOperator = mOperator;
        dest.mContent = mContent.clone();
        dest.mContentList = mContentList;
    }

    @Override
    public OperatorType getEventType()
    {
        return mType;
    }

    @Override
    public IError.Type getErrorType()
    {
        return mErrType;
    }

    @Override
    public <V,
            A,
            R> void produce(OperatorType t,
                            IPair content,
                            IOperator<V,
                                      A,
                                      R> operator)
    {
        mErrType = NO_ERROR;
        mType = t;
        mContent = content;
        mOperator = operator;
        mContentList = null;
    }

    @Override
    public void produce(OperatorType t, List<ITriple> cp)
    {
        mErrType = NO_ERROR;
        mType = t;
        mContent = null;
        mOperator = null;
        mContentList = cp;
    }

    @Override
    public <E,
            H,
            R> void error(IError.Type t,
                          IPair content,
                          IOperator<E,
                                    H,
                                    R> operator)
    {
        mType = OperatorType.NULL;
        mErrType = t;
        mContent = content;
        mOperator = operator;
        mContentList = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V,
            A,
            R> IOperator<V,
                         A,
                         R> getEventOp()
    {
        return (IOperator<V,
                          A,
                          R>) mOperator;
    }

    @Override
    public IPair getContent()
    {
        return mContent;
    }

    @Override
    public List<ITriple> getContentList()
    {
        return mContentList;
    }

    private static class QEventFactory
            implements
            EventFactory<QEvent>
    {
        @Override
        public QEvent newInstance()
        {
            return new QEvent();
        }
    }

}
