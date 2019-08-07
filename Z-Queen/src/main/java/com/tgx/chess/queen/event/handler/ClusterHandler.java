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
package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author William.d.zk
 */
public class ClusterHandler<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{

    private final QueenManager<C>    _QueenManager;
    private final RingBuffer<QEvent> _Error;
    private final RingBuffer<QEvent> _Writer;
    private final Logger             _Log = Logger.getLogger(getClass().getName());

    public ClusterHandler(QueenManager<C> queenManager,
                          RingBuffer<QEvent> error,
                          RingBuffer<QEvent> writer)
    {
        _QueenManager = queenManager;
        _Error = error;
        _Writer = writer;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            switch (event.getErrorType())
            {
                case ACCEPT_FAILED:
                case CONNECT_FAILED:
                    _Log.info(String.format("error type %s,ignore ", event.getErrorType()));
                    event.ignore();
                    break;
                default:
                    _Log.warning("cluster io error , do close session");
                    IPair errorContent = event.getContent();
                    ISession<C> session = errorContent.second();
                    IOperator<Void,
                              ISession<C>,
                              Void> closeOperator = event.getEventOp();
                    ISessionDismiss<C> dismiss = session.getDismissCallback();
                    boolean closed = session.isClosed();
                    closeOperator.handle(null, session);
                    if (!closed) {
                        dismiss.onDismiss(session);
                    }
            }
        }
        else {
            switch (event.getEventType())
            {
                case CONNECTED:
                    IPair connectedContent = event.getContent();
                    IConnectActivity<C> connectActivity = connectedContent.first();
                    AsynchronousSocketChannel channel = connectedContent.second();
                    IOperator<IConnectActivity<C>,
                              AsynchronousSocketChannel,
                              ITriple> connectedOperator = event.getEventOp();
                    ITriple connectedHandled = connectedOperator.handle(connectActivity, channel);
                    //connectedHandled 不可能为 null
                    IControl[] waitToSend = connectedHandled.first();
                    ISession<C> session = connectedHandled.second();
                    IOperator<IControl[],
                              ISession,
                              ITriple> sendTransferOperator = connectedHandled.third();
                    event.produce(WRITE, new Pair<>(waitToSend, session), sendTransferOperator);
                    connectActivity.getSessionCreated()
                                   .onCreate(session);
                    _Log.info(String.format("cluster link mappingHandle %s,connected", session));
                    break;
                case LOGIC:
                    //TODO  逻辑处理器需要在此处完成操作。
                    break;
                default:
                    _Log.warning(String.format("cluster link mappingHandle can't mappingHandle %s",
                                               event.getEventType()));
                    break;
            }
        }
    }
}
/*        ,IConsistentWrite

{

    final QueenManager _QueenManager;

    public ClusterHandler(final QueenManager queenManager) {
        _QueenManager = queenManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.hasError()) {}
        {
            switch (event.getEventType()) {
                case CONNECTED:
                    IEventOp<Pair<ClusterNode<E, D, N>, IConnectMode.ZOperators>,
                             AsynchronousSocketChannel> cOperator = event.getEventOp();// NODE_CONNECTED
                    Pair<Pair<ClusterNode<E, D, N>, IConnectMode.ZOperators>, AsynchronousSocketChannel> cContent = event.getContent();
                    Pair<ClusterNode<E, D, N>, IConnectMode.ZOperators> nmPair = cContent.first();
                    // 集群至少2台机器的时候才需要进行诸多网络操作。
                    AsynchronousSocketChannel channel = cContent.second();
                    Triple<IControl, ISession, IEventOp<IControl, ISession>> cResult = cOperator.mappingHandle(nmPair, channel);
                    IControl inCmd = cResult.first();
                    if (inCmd != null && inCmd.getSerialNum() != XF000_NULL.COMMAND) publish(_WriteRB,
                                                                                             Type.DISPATCH,
                                                                                             inCmd,
                                                                                             cResult.second(),
                                                                                             cResult.third());
                    break;
                case CLOSE:
                    IEventOp<Void, ISession> ccOperator = event.getEventOp();
                    Pair<Void, ISession> ccContent = event.getContent();
                    ISession session = ccContent.second();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    if (dismiss != null && !session.isClosed()) dismiss.onDismiss(session);
                    ccOperator.mappingHandle(null, session);
                    _ClusterNode.clearSession(session);
                    _ClusterNode.rmSession(session);
                    break;
                case LOCAL:
                    Pair<IControl, AioSessionManager> lContent = event.getContent();
                    IEventOp<IControl, AioSessionManager> lcOperator = event.getEventOp();// NODE_LOCAL
                    List<Triple<IControl,
                                ISession,
                                IEventOp<IControl, ISession>>> lcResult = lcOperator.handleResultAsList(lContent.first(),
                                                                                                        lContent.second());
                    if (lcResult != null) for (Triple<IControl, ISession, IEventOp<IControl, ISession>> llt : lcResult) {
                        publish(_WriteRB, Type.DISPATCH, llt.first(), llt.second(), llt.third());
                    }
                    break;
                case LOGIC:
                    // Cluster bind Session
                    IEventOp<IControl, ISession> lOperator = event.getEventOp();// DEFAULT_TRANSFER_LOGIC
                    Pair<IControl, ISession> rContent = event.getContent();
                    inCmd = rContent.first();
                    session = rContent.second();
                    List<IControl> wList = new LinkedList<>();
                    switch (inCmd.getSerialNum()) {
                        case X10_StartElection.COMMAND:
                            X10_StartElection x10 = (X10_StartElection) inCmd;
                            wList.add(_ClusterNode.onReceiveElection(x10.nodeId,
                                                                     x10.termId,
                                                                     x10.slotIndex,
                                                                     x10.lastCommittedTermId,
                                                                     x10.lastCommittedSlotIndex));
                            break;
                        case X11_Ballot.COMMAND:
                            X11_Ballot x11 = (X11_Ballot) inCmd;
                            _ClusterNode.onReceiveBallot(x11.nodeId, x11.termId, x11.slotIndex, x11.ballotId, x11.accept);
                            break;
                        case X12_AppendEntity.COMMAND:
                            X12_AppendEntity x12 = (X12_AppendEntity) inCmd;
                            wList.add(_ClusterNode.onReceiveEntity(x12.getLeaderId(),
                                                                   x12.getLeaderCommittedSlotIndex(),
                                                                   (LogEntry<E>) x12.getEntry()));
                            break;
                        case X13_EntryAck.COMMAND:
                            X13_EntryAck x13 = (X13_EntryAck) inCmd;
                            _ClusterNode.onReceiveEntryAck(wList,
                                                           x13.nodeId,
                                                           x13.termId,
                                                           x13.slotIndex,
                                                           x13.nextIndex,
                                                           x13.accept,
                                                           x13.qualify);
                            break;
                        case X15_CommitEntry.COMMAND:
                            X15_CommitEntry x1A = (X15_CommitEntry) inCmd;
                            wList.add(_ClusterNode.onReceiveCommit(x1A.nodeId, x1A.termId, x1A.slotIndex, x1A.idempotent));
                            break;
                        case X17_ClientEntry.COMMAND:
                            X17_ClientEntry x1C = (X17_ClientEntry) inCmd;
                            LogEntry<E> clientLogEntry = new LogEntry<>();
                            clientLogEntry.decode(x1C.getPayload());
                            _ClusterNode.onReceiveClientEntity(wList, x1C.nodeId, clientLogEntry);
                            break;
                        case X18_ClientEntryAck.COMMAND:
                            X18_ClientEntryAck x18 = (X18_ClientEntryAck) inCmd;
                            wList.add(_ClusterNode.onReceiveEntryAck(x18.nodeId,
                                                                     x18.termId,
                                                                     x18.slotIndex,
                                                                     x18.lastCommittedSlotIndex,
                                                                     x18.clientSlotIndex));
                            break;
                        case X19_LeadLease.COMMAND:
                            X19_LeadLease x19 = (X19_LeadLease) inCmd;
                            wList.add(_ClusterNode.onReceiveLease(x19.nodeId, x19.termId, x19.slotIndex));
                            break;
                        case X103_Close.COMMAND:
                            dismiss = session.getDismissCallback();
                            if (dismiss != null && !session.isClosed()) dismiss.onDismiss(session);
                            getCloseOperator.INSTANCE.mappingHandle(null, session);
                            _ClusterNode.clearSession(session);
                            _ClusterNode.rmSession(session);
                            break;
                        case X104_ExchangeIdentity.COMMAND:
                            X104_ExchangeIdentity x104 = (X104_ExchangeIdentity) inCmd;
                            long identity = x104.getNodeIdentity();
                            long _clusterId = identity & QueenCode._IndexHighMask;
                            long _XID_TYPE = _clusterId & QueenCode.XID_MK;
                            _ClusterNode.mapSession(identity, session, _clusterId, _XID_TYPE);
                            _ClusterNode.onClusterConnected(_clusterId);
                            break;
                        default:
                            Triple<IControl, ISession, IEventOp<IControl, ISession>> rResult = lOperator.mappingHandle(inCmd, session);
                            if (rResult.first()
                                       .getSerialNum() != XF000_NULL.COMMAND) publish(_WriteRB, Type.DISPATCH, rResult.first(), rResult.second(), rResult.third());
                            break;
                    }
                    for (IControl outCommand : wList)
                        switch (outCommand.getSerialNum()) {
                            case XF000_NULL.COMMAND:
                                break;// drop
                            case XF001_TransactionCompleted.COMMAND:
                                publish(_ConsistentResultRB, Type.BRANCH, outCommand, null, null);
                                break;
                            default:
                                ISession oSession = outCommand.getSession();
                                publish(_WriteRB,
                                        Type.DISPATCH,
                                        outCommand,
                                        oSession == null ? session : oSession,
                                        ENCODER_OPERATOR.PLAIN_SYMMETRY);
                                break;
                        }
                    break;
                case BRANCH:
                    Pair<IControl, ISession> bContent = event.getContent();
                    IEventOp<IControl, ISession> bOperator = event.getEventOp();
                    inCmd = bContent.first();
                    session = bContent.second();
                    Collection<IControl> rCollection = consistentWrite(inCmd, _ClusterNode, session, inCmd.getTransactionKey());
                    if (rCollection != null) for (IControl out : rCollection)
                        switch (out.getSerialNum()) {
                            case XF001_TransactionCompleted.COMMAND:
                                tryPublish(_ConsistentResultRB, Type.BRANCH, out, null, bOperator);
                                break;
                            default:
                                ISession oSession = out.getSession();
                                publish(_WriteRB, Type.DISPATCH, out, oSession == null ? session : oSession, ENCODER_OPERATOR.PLAIN_SYMMETRY);
                                break;
                        }
                    break;
                default:
                    break;
            }
        }
        else {
            IError.Type errorType = event.getErrorType();
            switch (errorType) {
                case CONNECT_FAILED:
                    IEventOp<Pair<ClusterNode<E, D, N>, Throwable>, IConnectActivity> cOperator = event.getEventOp();
                    Pair<Pair<ClusterNode<E, D, N>, Throwable>, IConnectActivity> cContent = event.getContent();
                    cOperator.mappingHandle(cContent.first(), cContent.second());
                    ClusterNode<E, D, N> clusterNode = cContent.first()
                                                               .first();
                    clusterNode.getErrorOperator(cContent.second());
                    break;
                case CLOSED:// CLOSE_ERROR_OPERATOR
                    IEventOp<Throwable, ISession> ccOperator = event.getEventOp();
                    Pair<Throwable, ISession> ccContent = event.getContent();
                    Throwable throwable = ccContent.first();
                    ISession session = ccContent.second();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    if (dismiss != null && !session.isClosed()) dismiss.onDismiss(session);
                    ccOperator.mappingHandle(throwable, session);
                    _ClusterNode.clearSession(session);
                    _ClusterNode.rmSession(session);
                    break;
                default:
                    IEventOp<Throwable, ISession> eOperator = event.getEventOp();
                    Pair<Throwable, ISession> eContent = event.getContent();
                    eOperator.mappingHandle(eContent.first(), eContent.second());// LOG_OPERATOR
                    break;
            }
        }
        event.reset();
    }

    public final ClusterNode<E, D, N> getNode() {
        return _ClusterNode;
    }

    @Override
    public final RESULT trial(IControl cmd, IConnectMode.ZOperators mode) {
        if (mode.equals(ZOperators.CLUSTER_CONSUMER) || mode.equals(ZOperators.CLUSTER_SERVER)) {
            switch (cmd.getSerialNum()) {
                case X10_StartElection.COMMAND:
                case X11_Ballot.COMMAND:
                case X12_AppendEntity.COMMAND:
                case X13_EntryAck.COMMAND:
                case X14_RSyncEntry.COMMAND:
                case X15_CommitEntry.COMMAND:
                case X16_CommittedAck.COMMAND:
                case X17_ClientEntry.COMMAND:
                case X18_ClientEntryAck.COMMAND:
                case X19_LeadLease.COMMAND:
                case X1A_LeaseAck.COMMAND:
                case X103_Close.COMMAND:
                case X104_ExchangeIdentity.COMMAND:
                    return RESULT.HANDLE;
            }
        }
        return RESULT.IGNORE;
    }

}
*/