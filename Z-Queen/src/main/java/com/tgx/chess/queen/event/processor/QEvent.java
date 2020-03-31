/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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
package com.tgx.chess.queen.event.processor;

import java.util.List;

import com.lmax.disruptor.EventFactory;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.IReset;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IEvent;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IOperator.Type;

/**
 * @author William.d.zk
 */
public class QEvent
        implements
        IReset,
        IEvent
{
    public static final EventFactory<QEvent> EVENT_FACTORY = new QEventFactory();
    private IError.Type                      mErrType      = IError.Type.NO_ERROR;
    private IOperator.Type                   mType         = IOperator.Type.NULL;
    private IPair                            mContent;
    private IOperator<?,
                      ?,
                      ?>                     mOperator;
    private List<IPair>                      mContentList;

    @Override
    public String toString()
    {
        return String.format("\nERR: %s\nTP:%s\nOP:%s\nCTL:\n%s\nCT:\n%s",
                             mErrType,
                             mType,
                             mOperator,
                             mContentList,
                             mContent);
    }

    @Override
    public void reset()
    {
        mType = IOperator.Type.NULL;
        mErrType = IError.Type.NO_ERROR;
        mOperator = null;
        mContent = null;
        mContentList = null;
    }

    public void ignore()
    {
        mType = Type.IGNORE;
        mErrType = IError.Type.NO_ERROR;
        mContent = null;
        mOperator = null;
        mContentList = null;
    }

    public void terminate()
    {
        mType = Type.TERMINATE;
        mErrType = IError.Type.NO_ERROR;
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
    public IOperator.Type getEventType()
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
            R> void produce(IOperator.Type t,
                            IPair content,
                            IOperator<V,
                                      A,
                                      R> operator)
    {
        mErrType = IError.Type.NO_ERROR;
        mType = t;
        mContent = content;
        mOperator = operator;
        mContentList = null;
    }

    @Override
    public void produce(IOperator.Type t, List<IPair> cp)
    {
        mErrType = IError.Type.NO_ERROR;
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
        mType = IOperator.Type.NULL;
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
    public List<IPair> getContentList()
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
