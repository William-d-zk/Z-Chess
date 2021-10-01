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

package com.isahl.chess.knight.raft.component;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.service.RaftPeer;
import com.isahl.chess.queen.events.cluster.IConsistencyReject;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISessionManager;

import static com.isahl.chess.knight.raft.model.RaftCode.WAL_FAILED;

public class ConsistencyReject
        implements IConsistencyReject
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final RaftPeer        _RaftPeer;
    private final ISessionManager _SessionManager;

    public ConsistencyReject(RaftPeer peer, ISessionManager manager)
    {
        _RaftPeer = peer;
        _SessionManager = manager;
    }

    @Override
    public ITriple resolve(IProtocol request, long origin)
    {
        _Logger.debug("reject: peer[%#x],origin[%#x]", _RaftPeer.getPeerId(), origin);
        if(!ZUID.isTypeOfCluster(origin)) {
            ISession session = _SessionManager.findSessionByIndex(origin);
            //执行关闭的session 属于 customer
            if(session != null) {
                return new Triple<>(_RaftPeer.raftResp(WAL_FAILED,
                                                       _RaftPeer.getPeerId(),
                                                       origin,
                                                       request.serial(),
                                                       request.encode()), session, session.getError());
            }
        }
        return null;
    }
}
