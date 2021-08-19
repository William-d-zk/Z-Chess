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
package com.isahl.chess.queen.io.core.inf;

import com.isahl.chess.king.topology.ZUID;

import java.util.Collection;

/**
 * @author William.d.zk
 * 
 * @date 2018/4/8
 */
public interface ISessionManager
{
    long INVALID_INDEX = ~ZUID.TYPE_MASK;
    long NULL_INDEX    = 0;

    /**
     * 通过session的唯一编号管理session
     * 凡是带有登录功能的都必须使用此方法进行管理。
     * prefix 近似 PortChannel 的结构
     * 
     * @param sessionIdx
     *            session.index
     * @param session
     *            mapping session
     * @param prefixArray
     *            session prefix array
     *            prefix 必须满足从H→L的匹配原则
     * 
     * @return old mapping session
     */
    ISession mapSession(long sessionIdx, ISession session, long... prefixArray);

    /**
     * 
     * @param prefix
     * 
     * @return
     */
    ISession findSessionByPrefix(long prefix);

    /**
     * 
     * @param prefix
     * 
     * @return
     */
    Collection<ISession> findAllByPrefix(long prefix);

    /**
     * 
     * @param index
     * 
     * @return
     */
    ISession findSessionByIndex(long index);

    /**
     * 获取当前前缀归属下所有session的数量
     * 
     * @param prefix
     * 
     * @return count
     */
    int getSessionCountByPrefix(long prefix);

    /**
     * 删除index→session 映射关系
     * 
     * @param index
     */
    ISession clearSession(long index);

    /**
     * 删除所有 prefix→sessions的关系
     * 
     * @param prefix
     * 
     * @return
     */
    Collection<ISession> clearAllSessionByPrefix(long prefix);

    default void clearSession(ISession session)
    {
        clearSession(session.getIndex());
    }

    void addSession(ISession session);

    void rmSession(ISession session);
}
