/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.z.queen.event.handler;

import static com.tgx.z.queen.event.inf.IOperator.Type.WRITE;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

import com.lmax.disruptor.RingBuffer;
import com.tgx.z.king.base.log.Logger;
import com.tgx.z.king.base.util.Pair;
import com.tgx.z.king.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.inf.IPipeEventHandler;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.IConnectionContext;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.core.inf.ISessionCreated;
import com.tgx.z.queen.io.core.inf.ISessionDismiss;
import com.tgx.z.queen.io.core.manager.QueenManager;

/**
 * @author William.d.zk
 */
public class LinkHandler
        implements
        IPipeEventHandler<QEvent, QEvent>
{
    private final Logger             _Log = Logger.getLogger(getClass().getName());
    private final RingBuffer<QEvent> _Error;
    private final RingBuffer<QEvent> _Writer;
    private final QueenManager       _QueenManager;

    public LinkHandler(QueenManager manager, RingBuffer<QEvent> error, RingBuffer<QEvent> writer) {
        _Error = error;
        _Writer = writer;
        _QueenManager = manager;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.hasError()) {
            switch (event.getErrorType()) {
                case ACCEPT_FAILED:
                case CONNECT_FAILED:
                    _Log.info(String.format("error type %s,ignore ", event.getErrorType()));
                    event.ignore();
                    break;
                default:
                    _Log.warning("server io error , do close session");
                    IOperator<Void, ISession> closeOperator = event.getEventOp();
                    Pair<Void, ISession> errorContent = event.getContent();
                    ISession session = errorContent.second();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    boolean closed = session.isClosed();
                    closeOperator.handle(null, session);
                    if (!closed) dismiss.onDismiss(session);
            }
        }
        else {
            switch (event.getEventType()) {
                case CONNECTED:
                    IOperator<IConnectionContext, AsynchronousSocketChannel> connectedOperator = event.getEventOp();
                    Pair<IConnectionContext, AsynchronousSocketChannel> connectedContent = event.getContent();
                    AsynchronousSocketChannel channel = connectedContent.second();
                    Triple<ICommand[],
                           ISession,
                           IOperator<ICommand[], ISession>> connectedHandled = connectedOperator.handle(connectedContent.first(), channel);
                    //connectedHandled 不可能为 null
                    ICommand[] waitToSends = connectedHandled.first();
                    ISession session = connectedHandled.second();
                    ISessionCreated sessionCreated = connectedContent.first()
                                                                     .getSessionCreated();
                    sessionCreated.onCreate(session);
                    IOperator<ICommand[], ISession> sendOperator = connectedHandled.third();
                    if (Objects.nonNull(waitToSends)) {
                        publish(_Writer, WRITE, waitToSends, session, sendOperator);
                    }
                    break;
            }
        }
        event.reset();
    }
}
/*        ,IConsistentRead<E, D, N>
{

    private final ConcurrentSkipListMap<Long, ConsistentTransaction<ISession>> _TransactionSkipListMap = new ConcurrentSkipListMap<>();
    private final QueenManager                                                 _QueenManager;

    public LinkHandler(QueenManager queenManager) {
        _QueenManager = queenManager;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.noError()) {
            switch (event.getEventType()) {
                case CONNECTED:
                    IEventOp<Pair<N, MODE>, AsynchronousSocketChannel> cOperator = event.getEventOp();
                    Pair<Pair<N, IConnectMode.MODE>, AsynchronousSocketChannel> cContent = event.getContent();
                    Pair<N, IConnectMode.MODE> nmPair = cContent.first();
                    AsynchronousSocketChannel channel = cContent.second();
                    Triple<ICommand, AioSession, IEventOp<ICommand, AioSession>> cResult = cOperator.handle(nmPair, channel);
                    ICommand cmd = cResult.first();
                    if (cmd != null && cmd.getSerialNum() != XF000_NULL.COMMAND) {
                        publish(_WriteRB, Type.DISPATCH, cmd, cResult.second(), cResult.third());
                    }
                    break;
                case CLOSE:
                    IEventOp<Void, ISession> ccOperator = event.getEventOp();
                    Pair<Void, ISession> ccContent = event.getContent();
                    ISession session = ccContent.second();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    if (dismiss != null && !session.isClosed()) dismiss.onDismiss(session);
                    ccOperator.handle(null, session);
                    _BizNode.clearSession(session);
                    _BizNode.rmSession(session);
                    break;
                case LOCAL:
                    Pair<ICommand, AioSessionManager> lcContent = event.getContent();
                    IEventOp<ICommand, AioSessionManager> lcOperator = event.getEventOp();
                    List<Triple<ICommand,
                                ISession,
                                IEventOp<ICommand, ISession>>> lcResult = lcOperator.handleResultAsList(lcContent.first(),
                                                                                                        lcContent.second());
                    if (lcResult != null) for (Triple<ICommand, ISession, IEventOp<ICommand, ISession>> llt : lcResult) {
                        publish(_WriteRB, Type.DISPATCH, llt.first(), llt.second(), llt.third());
                    }
                    break;
                case LOGIC:
                    Pair<ICommand, ISession> rContent = event.getContent();
                    cmd = rContent.first();
                    session = rContent.second();
                    IEventOp<ICommand, ISession> rOperator = event.getEventOp();
                    Triple<RESULT, ICommand, Throwable> lResult = localRead(cmd, _BizNode.getBizDao());
                    convergentClearTransaction();
                    switch (lResult.first()) {
                        case HANDLE:
                            Triple<ICommand, ISession, IEventOp<ICommand, ISession>> rResult = rOperator.handle(cmd, session);
                            publish(_WriteRB, Type.DISPATCH, rResult.first(), rResult.second(), rResult.third());
                            break;
                        case SECTION:
                            rResult = rOperator.handle(cmd, session);
                            publish(_WriteRB, Type.DISPATCH, rResult.first(), rResult.second(), rResult.third());
                        case PASS:
                            ICommand cCmd = lResult.second();
                            long transactionKey = ClusterNode.getUniqueIdentity();
                            cCmd.setTransactionKey(transactionKey);
                            _BizNode.mapSession(transactionKey, session);
                            _TransactionSkipListMap.put(transactionKey,
                                                        new ConsistentTransaction<>(transactionKey,
                                                                                    TimeUtil.CURRENT_TIME_SECOND_CACHE + 3,
                                                                                    session));
                            tryPublish(_ConsistentWriteRB, Type.BRANCH, cCmd, session, rOperator);
                            break;
                        case IGNORE:
                            switch (cmd.getSerialNum()) {
                                case X103_Close.COMMAND:
                                    dismiss = session.getDismissCallback();
                                    if (dismiss != null && !session.isClosed()) dismiss.onDismiss(session);
                                    CLOSE_OPERATOR.INSTANCE.handle(null, session);
                                    _BizNode.clearSession(session);
                                    _BizNode.rmSession(session);
                                    break;
                                case X104_ExchangeIdentity.COMMAND:
                                    X104_ExchangeIdentity x104 = (X104_ExchangeIdentity) cmd;
                                    long identity = x104.getNodeIdentity();
                                    long clusterId = identity & QueenCode._IndexHighMask;
                                    long modeType = clusterId & QueenCode.XID_MK;
                                    _BizNode.mapSession(identity, session, clusterId, modeType);
                                    break;
                                case XF000_NULL.COMMAND:
                                    break;// drop
                                default:
                                    Triple<ICommand, ISession, IEventOp<ICommand, ISession>> mqResult = _BizNode.getMqServer()
                                                                                                                .consistentHandle(cmd,
                                                                                                                                  session);
                                    if (mqResult != null
                                        && mqResult.first()
                                                   .getSerialNum() != XF000_NULL.COMMAND) {
                                        publish(_WriteRB, Type.DISPATCH, mqResult.first(), mqResult.second(), mqResult.third());
                                    }
                                    break;
                            }
                            break;
                        case ERROR:
                            log.log(Level.WARNING, " consistent read error: @->" + cmd.toString(), lResult.third());
                            break;
                        default:
                            break;
                    }
                    break;
                case BRANCH:
                    Pair<ICommand, ISession> bContent = event.getContent();
                    IEventOp<ICommand, ISession> bOperator = event.getEventOp();
                    cmd = bContent.first();
                    Pair<ICommand, ISession> bResult = consistentRead(cmd.getTransactionKey(), _BizNode);
                    Triple<ICommand, ISession, IEventOp<ICommand, ISession>> rResult = bOperator.handle(bResult.first(), bResult.second());
                    publish(_WriteRB, Type.DISPATCH, rResult.first(), rResult.second(), rResult.third());
                    clearTransaction(cmd.getTransactionKey());
                    break;
                default:
                    break;
            }
        }
        else {
            IError.Type errorType = event.getErrorType();
            switch (errorType) {
                case CONNECT_FAILED:
                    IEventOp<Pair<IConnectError, Throwable>, IConnectActive> cOperator = event.getEventOp();
                    Pair<Pair<IConnectError, Throwable>, IConnectActive> cContent = event.getContent();
                    IConnectError connectError = cContent.first()
                                                         .first();
                    IConnectActive cActive = cContent.second();
                    connectError.getErrorOperator(cActive);
                    cOperator.handle(cContent.first(), cActive);
                    break;
                case CLOSED:
                    IEventOp<Throwable, ISession> ccOperator = event.getEventOp();
                    Pair<Throwable, ISession> ccContent = event.getContent();
                    ISession session = ccContent.second();
                    ISessionDismiss dismiss = session.getDismissCallback();
                    if (dismiss != null && !session.isClosed()) dismiss.onDismiss(session);
                    ccOperator.handle(ccContent.first(), session);
                    _BizNode.clearSession(session);
                    _BizNode.rmSession(session);
                    break;
                default:
                    IEventOp<Throwable, ISession> eOperator = event.getEventOp();
                    Pair<Throwable, ISession> eContent = event.getContent();
                    Throwable t = eContent.first();
                    session = eContent.second();
                    eOperator.handle(t, session);// LOG_OPERATOR
                    break;
            }

        }
        event.reset();
    }

    private void convergentClearTransaction() {
        if (_TransactionSkipListMap.isEmpty()) { return; }
        int convergentDepth = 2;
        for (int i = 0; i < convergentDepth; i++) {
            Map.Entry<Long, ConsistentTransaction<ISession>> firstEntry = _TransactionSkipListMap.firstEntry();
            if (firstEntry == null) {
                break;
            }
            ConsistentTransaction<ISession> transaction = firstEntry.getValue();
            if (transaction.isTimeout(TimeUtil.CURRENT_TIME_SECOND_CACHE)
                || !transaction.getEntity()
                               .isValid()) {
                _TransactionSkipListMap.pollFirstEntry();
                _BizNode.clearSession(transaction.getTransactionKey());
            }
            else {
                break;
            }
        }
    }

    private void clearTransaction(long transactionKey) {
        _BizNode.clearSession(transactionKey);
        _TransactionSkipListMap.remove(transactionKey);
        convergentClearTransaction();
    }

    @Override
    public RESULT trial(ICommand cmd, IConnectMode.MODE mode) {
        if (mode.equals(IConnectMode.MODE.SYMMETRY)) {
            switch (cmd.getSerialNum()) {
                case X103_Close.COMMAND:
                case X104_ExchangeIdentity.COMMAND:
                    return RESULT.HANDLE;
            }
        }
        else if (mode.equals(MODE.ACCEPT_SERVER) || mode.equals(MODE.ACCEPT_SERVER_SSL)) {
            if (cmd.getSerialNum() == X103_Close.COMMAND) return RESULT.HANDLE;
        }
        else if (_BizNode.getMqServer()
                         .trial(cmd, mode)
                         .equals(RESULT.HANDLE)) return RESULT.HANDLE;
        return RESULT.IGNORE;
    }

}
*/