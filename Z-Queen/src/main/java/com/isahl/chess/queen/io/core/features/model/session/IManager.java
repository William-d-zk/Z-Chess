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
package com.isahl.chess.queen.io.core.features.model.session;

import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

import java.util.Collection;

/**
 * @author William.d.zk
 * @date 2018/4/8
 */
public interface IManager
{
    long INVALID_INDEX = ~ZUID.TYPE_MASK;
    long NULL_INDEX    = 0;

    /**
     * 通过session的唯一编号管理session
     * 凡是带有登录功能的都必须使用此方法进行管理。
     * prefix 近似 PortChannel 的结构
     *
     * @param index       session.index
     * @param session     mapping session
     * @param prefixArray session prefix array
     *                    prefix 必须满足从H→L的匹配原则
     * @return old mapping session
     */
    ISession mapSession(long index, ISession session, long... prefixArray);

    void route(long index, long prefix);

    /**
     * 集群节点变更的时候必然会执行此动作
     *
     * @param prefix 注销 prefix
     *               这东西不能轻易使用，清理内存的代价非常高。
     */
    void dropPrefix(long prefix);

    /**
     * @param prefix session 聚合标签，使用前缀进行匹配。
     * @return session 从符合prefix 标签的集合中返回一个可用的 session
     */
    ISession findSessionByPrefix(long prefix);

    ISession findSessionOverIndex(long index);

    /**
     * @param prefix 前缀标签
     * @return collection 所有符合前缀标签的session
     */
    Collection<ISession> findAllByPrefix(long prefix);

    /**
     * @param index session 的 index，获取指定的 session
     *              在multi-bind的场景下需要注意多个index 可以关联相同的session
     * @return session index[1:1]的session
     */
    ISession findSessionByIndex(long index);

    /**
     * 获取当前前缀归属下所有session的数量
     *
     * @param prefix 前缀标签
     * @return count 符合前缀标签的session-count
     */
    int getSessionCountByPrefix(long prefix);

    /**
     * 删除index→session 映射关系
     *
     * @param index session-index
     */
    ISession clearIndex(long index);

    /**
     * 删除所有 prefix→sessions的关系
     *
     * @param prefix 前缀
     * @return collection 被解除关系的 session 集合
     */
    Collection<ISession> clearAllSessionByPrefix(long prefix);

    default void clearIndex(ISession session)
    {
        clearIndex(session.index());
    }

    void addSession(ISession session);

    void rmSession(ISession session);

    IoFactory<IProtocol> findIoFactoryBySerial(int factorySerial);




}
