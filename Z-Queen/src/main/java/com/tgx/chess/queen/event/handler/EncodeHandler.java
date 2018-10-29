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

package com.tgx.chess.queen.event.handler;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public class EncodeHandler
        implements
        ISessionHandler
{
    private final Logger _Log = Logger.getLogger(getClass().getName());

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            switch (event.getErrorType())
            {
                case FILTER_ENCODE:
                case ILLEGAL_STATE:
                case ILLEGAL_BIZ_STATE:
                default:
                    /*
                     透传到 EncodedHandler 去投递 _Error
                     */
                    break;
            }
        }
        else {
            switch (event.getEventType())
            {
                case WRITE:
                    IOperator<ICommand, ISession> writeOperator = event.getEventOp();
                    Pair<ICommand, ISession> pairWriteContent = event.getContent();
                    ICommand cmd = pairWriteContent.first();
                    ISession session = pairWriteContent.second();
                    encodeHandler(event, cmd, session, writeOperator);
                    cmd.dispose();
                    break;
                case WROTE:
                    IOperator<Integer, ISession> wroteOperator = event.getEventOp();
                    Pair<Integer, ISession> pairWroteContent = event.getContent();
                    int wroteCnt = pairWroteContent.first();
                    session = pairWroteContent.second();
                    encodeHandler(event, wroteCnt, session, wroteOperator);
                    break;
            }
        }
    }
}
