/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.bishop.io;

import com.isahl.chess.queen.io.core.inf.ICommand;

/**
 * @author william.d.zk
 * 
 * @date 2019-08-04
 */
public interface IRouter
{
    /**
     * generate message id
     * 
     * @return id
     */
    long nextId();

    /**
     * register message with state for session by id
     * 
     * @param stateMessage
     *            message with state
     * @param sessionIndex
     *            session index
     */
    void register(ICommand stateMessage, long sessionIndex);

    /**
     * feed back message state
     * 
     * @param stateMessage
     *            message with state
     * @param sessionIndex
     *            session index
     */
    void ack(ICommand stateMessage, long sessionIndex);

    /**
     * clean session state machine for message stack
     * 
     * @param sessionIndex
     *            session index
     */
    void clean(long sessionIndex);
}
