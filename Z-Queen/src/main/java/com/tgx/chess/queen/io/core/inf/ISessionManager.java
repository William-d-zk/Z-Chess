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
package com.tgx.chess.queen.io.core.inf;

/**
 * @author William.d.zk
 */
public interface ISessionManager<C extends IContext>
{
    long INVALID_INDEX = -1;
    long NULL_INDEX    = 0;

    boolean mapSession(long index, ISession<C> session);

    default boolean mapSession(ISession<C> session) {
        return mapSession(session.getIndex(), session);
    }

    boolean mapSession(long index, ISession<C> session, long... filter);

    boolean findPort(long portIdx);

    void clearSession(long index);

    void clearSessionWithPort(long index, ISession<C> session);

    default void clearSession(ISession<C> session) {
        clearSessionWithPort(session.getIndex(), session);
    }

    void addSession(ISession<C> session);

    void rmSession(ISession<C> session);

    ISession<C> findSessionByIndex(long index);

    ISession<C> findSessionByPort(long portIdx);

    int getSubPortCount(long portMask);

    int getPortCount(long portMask);

    long[][] portIndex(long portMask);

    //-2bit- 类型 10 server 11 cluster 01 internal 00 client
    int CLIENT_SLOT   = 0;
    int INTERNAL_SLOT = 1;
    int SERVER_SLOT   = 2;
    int CLUSTER_SLOT  = 3;

}
