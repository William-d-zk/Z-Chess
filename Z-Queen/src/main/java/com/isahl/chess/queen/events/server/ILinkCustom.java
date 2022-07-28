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

package com.isahl.chess.queen.events.server;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.queen.events.cluster.IConsistencyBackload;
import com.isahl.chess.queen.events.routes.IMappingCustom;
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public interface ILinkCustom
        extends IMappingCustom,
                IConsistencyBackload
{

    /**
     * Cluster->Link.notify(Cluster.consensus_result)
     *
     * @param manager  session - manager
     * @param request  需要执行一致性要求的请求
     * @param backload cluster 处理请求的返回结果
     * @return first: response, second:session, third:operator
     */
    List<ITriple> onConsistency(IManager manager, IProtocol request, IConsistency backload);

    /**
     * 当出现了关闭 session 的需要时
     * 除了session dismiss 之外，还需要对现有链路进行关闭
     * 触发映射处理机制
     *
     * @param session target session
     */
    void close(ISession session);

}
