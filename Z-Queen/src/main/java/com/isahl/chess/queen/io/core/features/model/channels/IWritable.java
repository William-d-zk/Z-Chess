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
package com.isahl.chess.queen.io.core.features.model.channels;

import com.isahl.chess.queen.io.core.features.model.content.IPacket;

import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.WritePendingException;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author William.d.zk
 */
public interface IWritable<A>
{
    WRITE_STATUS write(IPacket ps,
                       CompletionHandler<Integer, A> completionHandler) throws WritePendingException, NotYetConnectedException, ShutdownChannelGroupException, RejectedExecutionException;

    WRITE_STATUS writeNext(int wroteCnt,
                           CompletionHandler<Integer, A> completionHandler) throws WritePendingException, NotYetConnectedException, ShutdownChannelGroupException, RejectedExecutionException;

    enum WRITE_STATUS
    {
        CLOSED,
        IGNORE,
        UNFINISHED,
        IN_SENDING,
        FLUSHED
    }

}
