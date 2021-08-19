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

package com.isahl.chess.queen.event.handler.mix;

import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.queen.event.handler.IMappingCustom;
import com.isahl.chess.queen.event.handler.cluster.IConsistencyJudge;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;

import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public interface ILinkCustom
        extends IMappingCustom,
                IConsistencyJudge
{

    /**
     * Cluster->Link.notify(Cluster.consensus_result)
     *
     * @param manager  session - manager
     * @param response link发来 强一致的请求
     * @return response
     */
    List<ITriple> notify(ISessionManager manager, IControl response, long origin);

    /**
     * 当出现了关闭 session 的需要时
     * 除了session dismiss 之外，还需要对现有链路进行关闭
     * 触发映射处理机制
     *
     * @param session target session
     */
    void close(ISession session);

}
