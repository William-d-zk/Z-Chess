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

package com.isahl.chess.queen.event.handler.mix;

import java.util.List;

import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.queen.event.handler.IMappingCustom;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/20
 */
public interface ILinkCustom
        extends
        IMappingCustom,
        IConsistentCustom
{

    /*
     *
     * @param manager
     * 
     * @param session
     * 
     * @param content
     * 
     * @return pair
     * first: to_send_array 1:N
     * second: to_transfer
     * Link->Cluster, consensus
     * 
     * @throws Exception
     * handle error
     * IPair handle(ISessionManager<C> manager, ISession<C> session,
     * IControl<C> content) throws Exception;
     */

    /**
     * Cluster->Link.notify(Cluster.consensus_result)
     *
     * @param manager
     * @param request
     *            link发来 强一致的请求
     * 
     * @return response
     */
    List<ITriple> notify(ISessionManager manager, IControl request, long origin);

}
