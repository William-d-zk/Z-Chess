/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.pawn.endpoint.spring.device.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.QttContext;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.mqtt.control.X118_QttSubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X119_QttSuback;
import com.tgx.chess.bishop.io.mqtt.control.X11A_QttUnsubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X11B_QttUnsuback;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X108_Shutdown;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftNotify;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.model.DeviceEntity;
import com.tgx.chess.pawn.endpoint.spring.device.jpa.repository.IDeviceJpaRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.mix.ILinkCustom;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;
import com.tgx.chess.queen.io.core.inf.ITraceable;

@Component
public class LinkCustom
        implements
        ILinkCustom<ZContext>
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IDeviceJpaRepository _DeviceRepository;
    private final IQttRouter           _QttRouter;

    @Autowired
    public LinkCustom(IDeviceJpaRepository deviceRepository,
                      IQttRouter qttRouter)
    {
        _DeviceRepository = deviceRepository;
        _QttRouter = qttRouter;
    }

    /**
     *
     * @param manager
     * @param session
     * @param input
     * @return first | 需要直接转换成
     * @throws Exception
     */
    @Override
    public IPair handle(ISessionManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> input) throws Exception
    {
        switch (input.serial())
        {
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) input;
                DeviceEntity device = new DeviceEntity();
                device.setToken(x111.getClientId());
                device.setPassword(x111.getPassword());
                device.setUsername(x111.getUserName());
                device.setOperation(IStorage.Operation.OP_APPEND);
                device = _DeviceRepository.findByToken(x111.getClientId());
                X112_QttConnack x112 = new X112_QttConnack();
                x112.responseOk();
                /*--检查X112 是否正常--*/
                int[] supportVersions = QttContext.getSupportVersion()
                                                  .getSecond();
                if (Arrays.stream(supportVersions)
                          .noneMatch(version -> version == x111.getProtocolVersion()))
                {
                    x112.rejectUnacceptableProtocol();
                }
                else if (!x111.isClean() && x111.getClientIdLength() == 0) {
                    x112.rejectIdentifier();
                }
                /*--检查device 是否正确，验证账户密码--*/
                if (device == null) {
                    x112.rejectIdentifier();
                }
                else if (!device.getUsername()
                                .equalsIgnoreCase(x111.getUserName())
                         || !device.getPassword()
                                   .equals(x111.getPassword()))
                {
                    /*
                     * @see DeviceEntity
                     *      username >=8 && <=32
                     *      password >=5 && <=32
                     *      no_empty
                     */
                    x112.rejectNotAuthorized();
                }
                if (x112.isOk() && device != null) {
                    ISession<ZContext> old = manager.mapSession(device.primaryKey(), session);
                    if (old != null) {
                        X108_Shutdown x108 = new X108_Shutdown();
                        x108.setSession(old);
                        _Logger.info("re-login ok %s, wait for consistent notify", x111.getClientId());
                        return new Pair<>(new X108_Shutdown[] { x108 }, x111);
                    }
                    else {
                        _Logger.info("login check ok:%s, wait for consistent notify", x111.getClientId());
                        return new Pair<>(null, x111);
                    }
                }
                else {
                    _Logger.info("reject %s",
                                 x112.getCode()
                                     .name());
                    return new Pair<>(new X112_QttConnack[] { x112 }, null);
                }
            case X118_QttSubscribe.COMMAND:
            case X11A_QttUnsubscribe.COMMAND:
                _Logger.info("%s ,wait for consistent notify", input);
                return new Pair<>(null, input);
        }
        return null;
    }

    @Override
    public List<ITriple> notify(ISessionManager<ZContext> manager, IControl<ZContext> response, long origin)
    {
        /*
        origin 
        在非集群情况下是 request.session_index
        在集群处理时 x76 携带了cluster 领域的session_index 作为入参，并在此处转换为 request.session_index
         */
        IControl<ZContext> request;
        boolean byLeader;
        if (response.serial() == X76_RaftNotify.COMMAND) {
            /*
            raft_client -> Link, session belong to cluster
            ignore session
            */
            X76_RaftNotify x76 = (X76_RaftNotify) response;
            int cmd = x76.getSerial();
            request = ZSort.getCommandFactory(cmd)
                           .create(cmd);
            request.decode(x76.getPayload());
            byLeader = x76.byLeader();
        }
        else {
            /*
             single mode
             */
            request = response;
            byLeader = true;
        }
        Stream<IControl<ZContext>> handled = mappingHandle(manager, request, origin, byLeader);
        if (handled != null) {
            return handled.filter(Objects::nonNull)
                          .map(control -> new Triple<>(control,
                                                       control.getSession(),
                                                       control.getSession()
                                                              .getContext()
                                                              .getSort()
                                                              .getEncoder()))
                          .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void adjudge(IProtocol consensus)
    {

    }

    @Override
    public <T extends ITraceable & IProtocol> IOperator<T,
                                                        Throwable,
                                                        Void> getOperator()
    {
        return this::handle;
    }

    @Override
    public <T extends ITraceable & IProtocol> Void handle(T request, Throwable throwable)
    {
        return null;
    }

    private Stream<IControl<ZContext>> mappingHandle(ISessionManager<ZContext> manager,
                                                     IControl<ZContext> consensus,
                                                     long origin,
                                                     boolean byLeader)
    {
        ISession<ZContext> session = manager.findSessionByIndex(origin);
        switch (consensus.serial())
        {
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) consensus;
                _Logger.info("%s login ok -> %#x", x111.getClientId(), origin);
                if (byLeader) {
                    _Logger.info("adjudge %s,login state", x111.getClientId());
                }
                if (session != null) {
                    X112_QttConnack x112 = new X112_QttConnack();
                    x112.responseOk();
                    x112.setSession(session);
                    return Stream.of(x112);
                }
                break;
            case X118_QttSubscribe.COMMAND:
                X118_QttSubscribe x118 = (X118_QttSubscribe) consensus;
                List<Pair<String,
                          IQoS.Level>> topics_lv = x118.getTopics();
                if (byLeader) {
                    _Logger.info("adjudge subscribe for %s, state", topics_lv);
                }
                if (topics_lv != null) {
                    X119_QttSuback x119 = new X119_QttSuback();
                    x119.setMsgId(x118.getMsgId());
                    topics_lv.forEach(topic -> x119.addResult(_QttRouter.addTopic(topic, origin) ? topic.getSecond()
                                                                                                 : IQoS.Level.FAILURE));
                    if (session != null) {
                        x119.setSession(session);
                        _Logger.info("subscribe topic:%s", x118.getTopics());
                        return Stream.of(x119);
                    }
                }
                break;
            case X11A_QttUnsubscribe.COMMAND:
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) consensus;
                List<String> topics = x11A.getTopics();
                if (byLeader) {
                    _Logger.info("adjudge Unsubscribe for %s, state", topics);
                }
                if (topics != null) {
                    x11A.getTopics()
                        .forEach(topic -> _QttRouter.removeTopic(topic, origin));
                    if (session != null) {
                        X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                        x11B.setMsgId(x11A.getMsgId());
                        x11B.setSession(session);
                        _Logger.info("unsubscribe topic:%s", x11A.getTopics());
                        return Stream.of(x11B);
                    }
                }
                break;
        }
        return null;
    }
}
