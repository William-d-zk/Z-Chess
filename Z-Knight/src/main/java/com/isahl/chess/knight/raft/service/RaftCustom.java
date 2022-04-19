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

package com.isahl.chess.knight.raft.service;

import com.isahl.chess.bishop.protocol.zchat.model.command.raft.*;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X08_Identity;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.raft.features.IRaftMachine;
import com.isahl.chess.queen.events.cluster.IClusterCustom;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

import static com.isahl.chess.king.config.KingCode.SUCCESS;

public class RaftCustom
        implements IClusterCustom<IRaftMachine>
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final RaftPeer _RaftPeer;

    public RaftCustom(RaftPeer raftPeer)
    {
        _RaftPeer = raftPeer;
    }

    /**
     * @param manager  cluster 管理器 注意与 device 管理器的区分
     * @param session  来源 session
     * @param received 需要 raft custom 处理的内容
     * @return ITriple
     * fst : command implements 'IControl', BATCH:List of IControl ; SINGLE: IControl
     * snd : command implements 'IControl/IConsistent', 需要传递给LINK的内容，
     * 只有两个环节出现：
     * leader→link.adjudge(x79),用来进行全局状态更新的内容，Leader维持全状态最新
     * →client→link.notify(x77),用来向link反馈一致性请求的结果
     * trd : operator-type [SINGLE|BATCH]
     */
    @Override
    public ITriple handle(IManager manager, ISession session, IProtocol received)
    {
        /*
         * leader -> follow, self::follow
         * 由于x76.origin == request.session.sessionIndex
         * LinkCustom中专门有对应findSession的操作，所以此处不再执行此操作，且在LinkCustom中执行更为安全
         */
        /* 作为 client 收到 notify */
        switch(received.serial()) {
            // follower → elector
            case 0x70 -> {
                return _RaftPeer.onVote((X70_RaftVote) received, manager, session);
            }
            // elector → candidate
            case 0x71 -> {
                return _RaftPeer.onBallot((X71_RaftBallot) received, manager, session);
            }
            // leader → follower
            case 0x72 -> {
                return _RaftPeer.onAppend((X72_RaftAppend) received, session);
            }
            // follower → leader
            case 0x73 -> {
                return _RaftPeer.onAccept((X73_RaftAccept) received, manager);
            }
            // * → candidate
            case 0x74 -> {
                return _RaftPeer.onReject((X74_RaftReject) received, session);
            }
            // client → leader
            case 0x75 -> {
                return _RaftPeer.onRequest((X75_RaftReq) received, manager, session);
            }
            // client.received:x76
            case 0x76 -> {
                return _RaftPeer.onResponse((X76_RaftResp) received);
            }
            // leader → client
            case 0x77 -> {
                return _RaftPeer.onNotify((X77_RaftNotify) received, manager);
            }
            case 0x78 -> {
                //TODO
            }
            case 0x79 -> {
                //TODO
            }
            // peer *, behind in config → previous in config
            case 0x08 -> {
                X08_Identity x08 = (X08_Identity) received;
                long peerId = x08.getIdentity();
                long newIdx = x08.getSessionIdx();
                _Logger.debug("===> map peerId:%#x @ %#x", peerId, newIdx);
                manager.mapSession(newIdx, session, peerId);
            }
            default -> throw new IllegalStateException("Unexpected value: " + received.serial());
        }
        return null;
    }

    @Override
    public List<ITriple> onTimer(IManager manager, IRaftMachine machine)
    {
        return machine == null ? null : switch(machine.state()) {
            // step down → follower
            case FOLLOWER -> _RaftPeer.turnDown(machine);
            // vote
            case CANDIDATE -> _RaftPeer.vote4me(machine, manager);
            // heartbeat
            case LEADER -> _RaftPeer.logAppend(machine, manager);
            default -> null;
        };
    }

    @Override
    public List<ITriple> consistent(IManager manager, IoSerial request, long origin)
    {
        _Logger.debug("cluster consistent %s", request);
        return _RaftPeer.onSubmit(request.encode()
                                         .array(), manager, origin);
    }

    @Override
    public List<ITriple> changeTopology(IManager manager, IoSerial topology)
    {
        _Logger.debug("cluster new topology %s", topology);
        //Accept Machine State
        return _RaftPeer.onSubmit(topology.encode()
                                          .array(), manager, _RaftPeer.peerId());
    }

    @Override
    public boolean waitForCommit()
    {
        return _RaftPeer.isInCongress();
    }

    @Override
    public IConsistent skipConsistency(IoSerial request)
    {
        X76_RaftResp x76 = new X76_RaftResp();

        x76.setCode(SUCCESS);
        if(request instanceof IProtocol protocol) {
            long origin = protocol.session()
                                  .getIndex();
            x76.setOrigin(origin);
        }
        return x76;
    }
}
