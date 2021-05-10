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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.bishop.io.mqtt.QttContext;
import com.isahl.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.isahl.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.isahl.chess.bishop.io.mqtt.control.X118_QttSubscribe;
import com.isahl.chess.bishop.io.mqtt.control.X119_QttSuback;
import com.isahl.chess.bishop.io.mqtt.control.X11A_QttUnsubscribe;
import com.isahl.chess.bishop.io.mqtt.control.X11B_QttUnsuback;
import com.isahl.chess.bishop.io.mqtt.control.X11E_QttDisconnect;
import com.isahl.chess.bishop.io.mqtt.handler.IQttRouter;
import com.isahl.chess.bishop.io.mqtt.handler.QttRouter;
import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.control.X102_Close;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.control.X108_Shutdown;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.X76_RaftNotify;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.knight.cluster.model.ConsistentProtocol;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceEntity;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.isahl.chess.queen.io.core.inf.IQoS;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;
import com.isahl.chess.queen.io.core.inf.ITraceable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class LinkCustom
        implements
        ILinkCustom
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IQttRouter    _QttRouter = new QttRouter();
    private final DeviceService _DeviceService;

    @Autowired
    public LinkCustom(DeviceService durableService)
    {
        _DeviceService = durableService;
    }

    /**
     *
     * @param manager
     *            session 管理器
     * @param session
     *            当前处理的 session
     * @param input
     *            收到的消息
     * 
     * @return first | 当前Link链路上需要返回的结果，second | 需要进行一致性处理的结果
     * 
     * @throws Exception
     *             可能出现的执行异常
     */
    @Override
    public IPair handle(ISessionManager manager, ISession session, IControl input) throws Exception
    {
        /*--检查X112 是否正常--*/
        /*--检查device 是否正确，验证账户密码--*/
        switch (input.serial())
        {
            case X102_Close.COMMAND -> session.innerClose();
            case X111_QttConnect.COMMAND ->
                {
                    X111_QttConnect x111 = (X111_QttConnect) input;
                    DeviceEntity device = new DeviceEntity();
                    device.setToken(x111.getClientId());
                    device.setPassword(x111.getPassword());
                    device.setUsername(x111.getUserName());
                    device.setOperation(IStorage.Operation.OP_APPEND);
                    device = _DeviceService.findDeviceByToken(x111.getClientId());
                    X112_QttConnack x112 = new X112_QttConnack();
                    QttContext qttContext = session.getContext(QttContext.class);
                    qttContext.setVersion(x111.getVersion());
                    x112.setVersion(x111.getVersion());
                    x112.responseOk();
                    if (QttContext.isNoSupportVersion(x111.getVersion())) {
                        x112.rejectUnsupportedVersion();
                    }
                    else if (!x111.isClean() && x111.getClientIdLength() == 0) {
                        x112.rejectIdentifier();
                    }
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
                         * username >=8 && <=32
                         * password >=17 && <=32
                         * no_empty
                         */
                        x112.rejectNotAuthorized();
                    }
                    if (x112.isOk() && device != null) {
                        ISession old = manager.mapSession(device.primaryKey(), session);
                        if (old != null) {
                            X108_Shutdown x108 = new X108_Shutdown();
                            x108.setSession(old);
                            _Logger.info("re-login ok %s, wait for consistent notify", x111.getClientId());
                            return new Pair<>(new X108_Shutdown[]{x108}, x111);
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
                        return new Pair<>(new X112_QttConnack[]{x112}, null);
                    }
                }
            case X118_QttSubscribe.COMMAND, X11A_QttUnsubscribe.COMMAND ->
                {
                    _Logger.info("%s ,wait for consistent notify", input);
                    return new Pair<>(null, input);
                }
        }
        return null;
    }

    @Override
    public List<ITriple> notify(ISessionManager manager, IControl response, long origin)
    {
        /*
         * origin
         * 在非集群情况下是 client-request.session_index
         * 在集群处理时 x76 携带了cluster 领域的session_index 作为入参，并在此处转换为 client-request.session_index
         */
        IControl clientRequest;
        if (response.serial() == X76_RaftNotify.COMMAND) {
            /*
             * raft_client -> Link, session belong to cluster
             * ignore session
             */
            X76_RaftNotify x76 = (X76_RaftNotify) response;
            int cmd = x76.load();
            clientRequest = ZSortHolder.create(cmd);
            clientRequest.decode(x76.getPayload());
            _Logger.info("notify cluster client by leader %s", x76.byLeader());
        }
        else {
            /*
             * single mode
             */
            clientRequest = response;
            _Logger.info("notify client single mode");
        }
        ISession session = manager.findSessionByIndex(origin);
        switch (clientRequest.serial())
        {
            case X111_QttConnect.COMMAND ->
                {
                    X111_QttConnect x111 = (X111_QttConnect) clientRequest;
                    _Logger.info("%s login ok -> %#x", x111.getClientId(), origin);
                    if (x111.hasWill()) {
                        _QttRouter.will(x111.getWillTopic(), x111.getWillQoS(), origin, x111.isWillRetain());
                    }
                    if (x111.isClean()) {
                        _QttRouter.clean(origin);
                        _DeviceService.clean(origin);
                    }
                    else {
                        _DeviceService.loadHistory(origin);
                    }
                    if (session != null) {
                        QttContext qttContext = session.getContext(QttContext.class);
                        X112_QttConnack x112 = new X112_QttConnack();
                        x112.setVersion(qttContext.getVersion());
                        x112.responseOk();
                        x112.setSession(session);
                        return Collections.singletonList(new Triple<>(x112, session, session.getEncoder()));
                    }
                }
            case X118_QttSubscribe.COMMAND ->
                {
                    X118_QttSubscribe x118 = (X118_QttSubscribe) clientRequest;
                    Map<String,
                        IQoS.Level> subscribes = x118.getSubscribes();
                    if (subscribes != null) {
                        X119_QttSuback x119 = new X119_QttSuback();
                        x119.setMsgId(x118.getMsgId());
                        _DeviceService.subscribe(subscribes, origin, optional -> (topic, level) ->
                        {
                            IQoS.Level lv = _QttRouter.subscribe(topic, level, origin);
                            x119.addResult(lv);
                            optional.ifPresent(device -> device.addSubscribes(topic, level));
                        });
                        if (session != null) {
                            x119.setSession(session);
                            _Logger.info("subscribes :%s", x118.getSubscribes());
                            return Collections.singletonList(new Triple<>(x119, session, session.getEncoder()));
                        }
                    }
                }
            case X11A_QttUnsubscribe.COMMAND ->
                {
                    X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) clientRequest;
                    List<String> topics = x11A.getTopics();
                    if (topics != null) {
                        _DeviceService.unsubscribe(topics, origin, optional -> topic ->
                        {
                            _QttRouter.unsubscribe(topic, origin);
                            optional.ifPresent(device -> device.unsubscribe(topic));
                        });
                        if (session != null) {
                            X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                            x11B.setMsgId(x11A.getMsgId());
                            x11B.setSession(session);
                            _Logger.info("unsubscribe topic:%s", x11A.getTopics());
                            return Collections.singletonList(new Triple<>(x11B, session, session.getEncoder()));
                        }
                    }
                }
            case X11E_QttDisconnect.COMMAND ->
                {
                    _Logger.info("disconnect");
                    _QttRouter.disconnect(session.getIndex());
                    throw new ZException("service active close");
                }
        }
        return null;
    }

    @Override
    public void adjudge(IProtocol consensus)
    {
        _Logger.info("link custom by leader %s", consensus);
        switch (consensus.serial())
        {
            case X111_QttConnect.COMMAND ->
                {}
            case X118_QttSubscribe.COMMAND ->
                {}
            case X11A_QttUnsubscribe.COMMAND ->
                {}
        }
    }

    @Override
    public <T extends ITraceable & IProtocol> Void resolve(T request, Throwable throwable)
    {
        if (throwable == null) {
            _Logger.debug("notify---consistent");
            if (request.serial() == X76_RaftNotify.COMMAND) {
                _Logger.debug("cluster mode");
                X76_RaftNotify x76 = (X76_RaftNotify) request;
                byte[] data = x76.getPayload();
                if (x76.load() == ConsistentProtocol._SERIAL) {
                    ConsistentProtocol consistentProtocol = JsonUtil.readValue(data, ConsistentProtocol.class);
                    consistentProtocol.decode(data);
                    _Logger.debug("notify ok");
                }
                else {
                    _Logger.fetal("consistent notify failed");
                }
            }
            else {
                _Logger.debug("single mode");
                _Logger.debug("notify ok");
            }
        }
        else {
            _Logger.warning("request:%s", throwable, request);
        }
        return null;
    }

    @Bean
    public IQttRouter getQttRouter()
    {
        return _QttRouter;
    }
}
