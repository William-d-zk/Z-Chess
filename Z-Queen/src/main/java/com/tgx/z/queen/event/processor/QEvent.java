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
package com.tgx.z.queen.event.processor;

import java.util.List;

import com.lmax.disruptor.EventFactory;
import com.tgx.z.king.base.inf.IReset;
import com.tgx.z.king.base.util.Pair;
import com.tgx.z.king.base.util.Triple;
import com.tgx.z.queen.event.inf.IError;
import com.tgx.z.queen.event.inf.IEvent;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.inf.IOperator.Type;

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
    private Pair<?, ?>                       mContent;
    private IOperator<?, ?>                  mOperator;
    private List<?>                          mContentList;

    @Override
    public String toString() {
        return String.format("\nERR: %s\nTP:%s\nOP:%s\nCTL:\n%s\nCT:\n%s", mErrType, mType, mOperator, mContentList, mContent);
    }

    @Override
    public void reset() {
        mType = IOperator.Type.NULL;
        mErrType = IError.Type.NO_ERROR;
        mOperator = null;
        if (mContent != null) {
            mContent.dispose();
        }
        mContent = null;
        if (mContentList != null) {
            mContentList.clear();
            mContentList = null;
        }
    }

    public void transfer(QEvent dest) {
        dest.mType = mType;
        dest.mErrType = mErrType;
        dest.mOperator = mOperator;
        dest.mContent = mContent.clone();
        dest.mContentList = mContentList;
    }

    @Override
    public IOperator.Type getEventType() {
        return mType;
    }

    @Override
    public IError.Type getErrorType() {
        return mErrType;
    }

    @Override
    public <V, A> void produce(IOperator.Type t, V v, A a, IOperator<V, A> operator) {
        mErrType = IError.Type.NO_ERROR;
        mType = t;
        mContent = new Pair<>(v, a);
        mOperator = operator;
        mContentList = null;
    }

    @Override
    public <V, A> void produce(IOperator.Type t, List<Triple<V, A, IOperator<V, A>>> cp) {
        mErrType = IError.Type.NO_ERROR;
        mType = t;
        mContent = null;
        mOperator = null;
        mContentList = cp;
    }

    @Override
    public <E, H> void error(IError.Type t, E e, H h, IOperator<E, H> operator) {
        mType = IOperator.Type.NULL;
        mErrType = t;
        mContent = new Pair<>(e, h);
        mOperator = operator;
        mContentList = null;
    }

    public void ignore() {
        mType = Type.IGNORE;
        mErrType = IError.Type.NO_ERROR;
        mContent = null;
        mOperator = null;
        mContentList = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, A> IOperator<V, A> getEventOp() {
        return (IOperator<V, A>) mOperator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, A> Pair<V, A> getContent() {
        return (Pair<V, A>) mContent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, A> List<Triple<V, A, IOperator<V, A>>> getContentList() {
        return (List<Triple<V, A, IOperator<V, A>>>) mContentList;
    }

    private static class QEventFactory
            implements
            EventFactory<QEvent>
    {
        @Override
        public QEvent newInstance() {
            return new QEvent();
        }
    }

}
