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
import com.isahl.chess.bishop.io.mqtt.control.*;
import com.isahl.chess.bishop.io.mqtt.handler.IQttRouter;
import com.isahl.chess.bishop.io.mqtt.handler.QttRouter;
import com.isahl.chess.bishop.io.mqtt.v5.control.X11F_QttAuth;
import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.control.X102_Close;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.control.X108_Shutdown;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.X76_RaftResp;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.X79_RaftAdjudge;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.model.RaftCode;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.spi.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.spi.ILinkService;
import com.isahl.chess.queen.event.handler.mix.ILinkCustom;
import com.isahl.chess.queen.io.core.inf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class LinkCustom
        implements ILinkCustom
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IQttRouter     _QttRouter = new QttRouter();
    private final IDeviceService _DeviceService;
    private final ILinkService   _LinkService;

    @Autowired
    public LinkCustom(IDeviceService deviceService, ILinkService linkService)
    {
        _DeviceService = deviceService;
        _LinkService = linkService;
    }

    /**
     * @param manager session 管理器
     * @param session 当前处理的 session
     * @param input   收到的消息
     * @return first | 当前Link链路上需要返回的结果，second | 需要进行一致性处理的结果
     * @throws Exception 可能出现的执行异常
     */
    @Override
    public IPair handle(ISessionManager manager, ISession session, IControl input) throws Exception
    {
        /*--检查X112 是否正常--*/
        /*--检查device 是否正确，验证账户密码--*/
        switch(input.serial()) {
            case X102_Close.COMMAND -> session.innerClose();
            case X111_QttConnect.COMMAND -> {
                X111_QttConnect x111 = (X111_QttConnect) input;
                X112_QttConnack x112 = new X112_QttConnack();
                QttContext qttContext = session.getContext(QttContext.class);
                qttContext.setVersion(x111.getVersion());
                x112.setVersion(x111.getVersion());
                x112.responseOk();
                if(QttContext.isNoSupportVersion(x111.getVersion())) {
                    x112.rejectUnsupportedVersion();
                }
                else if(!x111.isClean() && x111.getClientIdLength() == 0) {
                    x112.rejectIdentifier();
                }
                long deviceId = ZUID.INVALID_PEER_ID;
                if(x112.isOk()) {
                    DeviceEntity device = _LinkService.findDeviceByToken(x111.getClientId());
                    if(device == null) {
                        x112.rejectIdentifier();
                    }
                    else if(!device.getUsername()
                                   .equalsIgnoreCase(x111.getUserName()) || !device.getPassword()
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
                    else {
                        deviceId = device.primaryKey();
                    }
                }
                if(x112.isOk()) {
                    ISession old = manager.mapSession(deviceId, session);
                    if(old != null) {
                        X108_Shutdown x108 = new X108_Shutdown();
                        x108.setSession(old);
                        _Logger.info("re-login ok %s, wait for consistent notify", x111.getClientId());
                        return new Pair<>(new X108_Shutdown[]{ x108 }, x111);
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
                    return new Pair<>(new X112_QttConnack[]{ x112 }, null);
                }
            }
            case X118_QttSubscribe.COMMAND, X11A_QttUnsubscribe.COMMAND, X11E_QttDisconnect.COMMAND -> {
                _Logger.info("%s ,wait for consistent notify", input);
                return new Pair<>(null, input);
            }
            case X11F_QttAuth.COMMAND -> {

            }
        }
        return null;
    }

    @Override
    public List<ITriple> notify(ISessionManager manager, IControl response, long origin)
    {

        IProtocol consensusBody;
        boolean isConsistency = true;
        switch(response.serial()) {
            case X76_RaftResp.COMMAND, X79_RaftAdjudge.COMMAND -> {
                if(response.serial() == X76_RaftResp.COMMAND) {
                    X76_RaftResp x76 = (X76_RaftResp) response;
                    isConsistency = x76.getCode() == RaftCode.SUCCESS.getCode();
                }
                int cmd = response.getSubSerial();
                consensusBody = ZSortHolder.create(cmd);
                consensusBody.decode(response.getPayload());
                _Logger.debug("consensus : %s", consensusBody);
            }
            default -> {
                /*
                 * single mode
                 */
                consensusBody = response;
                _Logger.info("notify client single mode");
            }
        }
        ISession session = manager.findSessionByIndex(origin);
        switch(consensusBody.serial()) {
            case X111_QttConnect.COMMAND -> {
                X111_QttConnect x111 = (X111_QttConnect) consensusBody;
                if(isConsistency) {
                    _Logger.info("%s login ok -> %#x", x111.getClientId(), origin);
                    if(x111.isClean()) {
                        _LinkService.clean(origin, _QttRouter);
                    }
                    else {
                        _LinkService.load(origin, _QttRouter);
                    }
                    _LinkService.onLogin(origin,
                                         x111.hasWill(),
                                         x111.getWillTopic(),
                                         x111.getWillQoS(),
                                         x111.isWillRetain(),
                                         x111.getWillMessage());
                }
                if(session != null) {
                    QttContext qttContext = session.getContext(QttContext.class);
                    X112_QttConnack x112 = new X112_QttConnack();
                    x112.setVersion(qttContext.getVersion());
                    if(isConsistency) {
                        x112.responseOk();
                    }
                    else {
                        x112.rejectServerUnavailable();
                    }
                    x112.setSession(session);
                    return Collections.singletonList(new Triple<>(x112, session, session.getEncoder()));
                }
            }
            case X118_QttSubscribe.COMMAND -> {
                X118_QttSubscribe x118 = (X118_QttSubscribe) consensusBody;
                Map<String, IQoS.Level> subscribes = x118.getSubscribes();
                if(subscribes != null && isConsistency) {
                    X119_QttSuback x119 = new X119_QttSuback();
                    x119.setMsgId(x118.getMsgId());
                    _LinkService.subscribe(subscribes, origin, optional->(topic, level)->{
                        IQoS.Level lv = _QttRouter.subscribe(topic, level, origin);
                        x119.addResult(lv);
                        optional.ifPresent(device->device.subscribe(topic, level));
                    });
                    if(session != null) {
                        x119.setSession(session);
                        _Logger.info("subscribes :%s", x118.getSubscribes());
                        return Collections.singletonList(new Triple<>(x119, session, session.getEncoder()));
                    }
                }
            }
            case X11A_QttUnsubscribe.COMMAND -> {
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) consensusBody;
                List<String> topics = x11A.getTopics();
                if(topics != null && isConsistency) {
                    _LinkService.unsubscribe(topics, origin, optional->topic->{
                        _QttRouter.unsubscribe(topic, origin);
                        optional.ifPresent(device->device.unsubscribe(topic));
                    });
                    if(session != null) {
                        X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                        x11B.setMsgId(x11A.getMsgId());
                        x11B.setSession(session);
                        _Logger.info("unsubscribe topic:%s", x11A.getTopics());
                        return Collections.singletonList(new Triple<>(x11B, session, session.getEncoder()));
                    }
                }
            }
            case X11E_QttDisconnect.COMMAND -> {
                if(session != null) {
                    if(_LinkService.offline(session.getIndex(), _QttRouter)) {
                        _Logger.info("shadow device offline %#x", session.getIndex());
                    }
                    else {
                        _Logger.warning("no login device → offline");
                    }
                    session.innerClose();
                }
                else {
                    _Logger.info("disconnect :%#x,session → null", origin);
                }
            }
            case X11F_QttAuth.COMMAND -> {

            }
        }
        return null;
    }

    @Override
    public void close(ISession session)
    {
        if((ZUID.TYPE_MASK & session.getIndex()) == ZUID.TYPE_CONSUMER) {
            _LinkService.offline(session.getIndex(), _QttRouter);
        }
    }

    @Override
    public <T extends IProtocol> T adjudge(IConsistent consistency, ISession session)
    {
        _Logger.debug("link custom by leader %s", consistency);
        switch(consistency.serial()) {
            case X76_RaftResp.COMMAND:
            case X79_RaftAdjudge.COMMAND:
                //TODO X79 是在 leader's Linker 进行处理，
        }




        /*
        int cmd = consensus.getSubSerial();
        IControl consensusBody = ZSortHolder.create(cmd);
        consensusBody.decode(consensus.getPayload());
        switch(consensusBody.serial()) {

        }
         */
        return (T) consistency;
    }

    @Bean
    public IQttRouter getQttRouter()
    {
        return _QttRouter;
    }
}
