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

package com.isahl.chess.pawn.endpoint.device.spi.plugin;

import com.isahl.chess.bishop.protocol.mqtt.command.*;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11D_QttPingresp;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11E_QttDisconnect;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X0A_Shutdown;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.pawn.endpoint.device.api.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.api.features.IMessageService;
import com.isahl.chess.pawn.endpoint.device.api.features.IStateService;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IRoutable;
import com.isahl.chess.queen.io.core.features.model.routes.IRouter;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.NULL;
import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.SINGLE;
import static com.isahl.chess.king.config.KingCode.SUCCESS;

/**
 * @author william.d.zk
 */
@Component
public class MQttPlugin
        implements IAccessService,
                   IRouter,
                   IThread,
                   IValid
{
    private final static Logger _Logger = Logger.getLogger("endpoint.pawn." + MQttPlugin.class.getName());

    private final IDeviceService  _DeviceService;
    private final IMessageService _MessageService;
    private final IStateService   _StateService;

    @Autowired
    public MQttPlugin(IDeviceService deviceService, IMessageService messageService, IStateService stateService)
    {
        _DeviceService = deviceService;
        _MessageService = messageService;
        _StateService = stateService;
    }

    @Override
    public boolean isSupported(IoSerial input)
    {
        return input.serial() >= 0x111 && input.serial() <= 0x11F;
    }

    @Override
    public List<ITriple> onLogic(IManager manager, ISession session, IProtocol content)
    {
        List<ITriple> results = null;
        switch(content.serial()) {
            case 0x113:
                X113_QttPublish x113 = (X113_QttPublish) content;
                if(x113.isRetain()) {
                    retain(x113.getTopic(), x113);
                }
                results = new LinkedList<>();
                switch(x113.getLevel()) {
                    case AT_LEAST_ONCE:
                        X114_QttPuback x114 = new X114_QttPuback();
                        x114.msgId(x113.msgId());
                        x114.with(session);
                        results.add(Triple.of(x114, session, session.getEncoder()));
                    case ALMOST_ONCE:
                        brokerTopic(manager, x113, broker(x113.getTopic()), results);
                        break;
                    case EXACTLY_ONCE:
                        X115_QttPubrec x115 = new X115_QttPubrec();
                        x115.msgId(x113.msgId());
                        x115.with(session);
                        x115.target(session.getIndex());
                        results.add(Triple.of(x115, session, session.getEncoder()));
                        register(x115.msgId(), x115);
                        _StateService.store(session.getIndex(), x113.msgId(), x113);
                        break;
                    default:
                        break;
                }
                break;
            case 0x114:
                //x113.QoS1 → client → x114, 服务端不存储需要client持有的消息
                X114_QttPuback x114 = (X114_QttPuback) content;
                ack(x114.msgId(), session.getIndex());
                _StateService.drop(session.getIndex(), x114.msgId());
                break;
            case 0x115:
                //x113.QoS2 → client → x115, 服务端恒定返回x116,Router无需操作。
                X115_QttPubrec x115 = (X115_QttPubrec) content;
                X116_QttPubrel x116 = new X116_QttPubrel();
                x116.msgId(x115.msgId());
                x116.target(session.getIndex());
                results = new LinkedList<>();
                results.add(Triple.of(x116.with(session), session, session.getEncoder()));
                register(x116.msgId(), x116);
                //                _StateService.drop(session.getIndex(), x115.getMsgId());
                break;
            case 0x116:
                //client → x113 → server → x115 → client → x116 → server , 服务端收到 x116,需要注意
                x116 = (X116_QttPubrel) content;
                X117_QttPubcomp x117 = new X117_QttPubcomp();
                x117.msgId(x116.msgId());
                results = new LinkedList<>();
                results.add(Triple.of(x117.with(session), session, session.getEncoder()));
                if(ack(x116.msgId(), session.getIndex())) {
                    if(_StateService.exists(session.getIndex(), x116.msgId())) {
                        MsgStateEntity received = _StateService.extract(session.getIndex(), x116.msgId());
                        if(received != null) {
                            X113_QttPublish n113 = new X113_QttPublish();
                            n113.with(session);
                            n113.setTopic(received.getTopic());
                            n113.withSub(received.payload());
                            n113.setLevel(IQoS.Level.EXACTLY_ONCE);
                            brokerTopic(manager, n113, broker(n113.getTopic()), results);
                        }
                    }
                }
                break;
            case 0x117:
                x117 = (X117_QttPubcomp) content;
                ack(x117.msgId(), session.getIndex());
                break;
            case 0x11C:
                return Collections.singletonList(Triple.of(new X11D_QttPingresp().with(session), session, session.getEncoder()));
        }
        return results;
    }

    @Override
    public ITriple onLink(IManager manager, ISession session, IProtocol input)
    {
        switch(input.serial()) {
            case 0x102 -> session.innerClose();
            case 0x111 -> {
                X111_QttConnect x111 = (X111_QttConnect) input;
                X112_QttConnack x112 = new X112_QttConnack();
                x112.with(session);
                x112.responseOk();
                if(QttContext.isNoSupportVersion(x111.getVersion())) {
                    x112.rejectUnsupportedVersion();
                }
                else if(!x111.isClean() && x111.getClientId() == null) {
                    x112.rejectIdentifier();
                }
                long deviceId = ZUID.INVALID_PEER_ID;
                if(x112.isOk()) {
                    DeviceEntity device = _DeviceService.findByToken(x111.getClientId());
                    if(device == null) {
                        x112.rejectIdentifier();
                    }
                    //@formatter:off
                    else if(!device.getUsername()
                                   .equalsIgnoreCase(x111.getUserName()) ||
                            !device.getPassword()
                                   .equals(x111.getPassword()))
                    //@formatter:on
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
                        _Logger.info("re-login ok, wait for consistent notify;client[%s]", x111.getClientId());
                        return Triple.of(new X0A_Shutdown().with(old), x111, SINGLE);
                    }
                    else {
                        _Logger.info("login check ok,wait for consistent notify;client[%s]", x111.getClientId());
                        return Triple.of(null, x111, NULL);
                    }
                }
                else {
                    _Logger.info("reject %s",
                                 x112.getCode()
                                     .name());
                    return Triple.of(x112, null, SINGLE);
                }
            }
            case 0x118, 0x11A, 0x11E -> {
                _Logger.info("link control [%s] ", input);
                return Triple.of(null, input, NULL);
            }
            case 0x11F -> {

            }
            default -> {
                return null;
            }
        }
        return null;
    }

    @Override
    public void onOffline(ISession session)
    {

    }

    @Override
    public List<ITriple> onConsistency(IManager manager, IConsistent backload, IoSerial consensusBody)
    {
        int code = backload.code();
        switch(consensusBody.serial()) {
            case 0x111 -> {
                X111_QttConnect x111 = (X111_QttConnect) consensusBody;
                X112_QttConnack x112 = new X112_QttConnack();
                ISession session = x111.session();
                if(session != null) {
                    x112.with(x111.session());
                    long origin = session.getIndex();
                    if(code == SUCCESS) {
                        _Logger.info("%s login ok -> %#x", x111.getClientId(), origin);
                        if(x111.isClean()) {
                            clean(origin);
                        }
                        x112.responseOk();
                        if(_StateService.onLogin(origin, x111.isClean(), x111.getKeepAlive())) {
                            x112.setPresent();
                        }
                    }
                    else {
                        x112.rejectServerUnavailable();
                    }
                    return Collections.singletonList(Triple.of(x112,
                                                               x112.session(),
                                                               x112.session()
                                                                   .getEncoder()));
                }
            }
            case 0x118 -> {
                X118_QttSubscribe x118 = (X118_QttSubscribe) consensusBody;
                Map<String, IQoS.Level> subscribes = x118.getSubscribes();
                ISession session = x118.session();
                if(subscribes != null && code == SUCCESS && session != null) {
                    X119_QttSuback x119 = new X119_QttSuback();
                    x119.with(session);
                    x119.msgId(x118.msgId());
                    long origin = session.getIndex();
                    subscribes.forEach((topic, level)->{
                        Topic t = new Topic(_QttTopicToRegex(topic), level, 0);
                        Subscribe subscribe = subscribe(t, origin);
                        if(subscribe != null) {
                            //TODO 统计单指令多个Subscribe的情况
                            x119.addResult(subscribe.level(origin));
                        }
                        else {
                            x119.addResult(level);
                        }
                    });
                    _Logger.info("subscribe topic:%s", x118.getSubscribes());
                    return Collections.singletonList(Triple.of(x119,
                                                               x119.session(),
                                                               x119.session()
                                                                   .getEncoder()));
                }
            }
            case 0x11A -> {
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) consensusBody;
                List<String> topics = x11A.getTopics();
                ISession session = x11A.session();
                if(topics != null && code == SUCCESS && session != null) {
                    long origin = session.getIndex();
                    topics.forEach(topic->unsubscribe(new Topic(_QttTopicToRegex(topic)), origin));
                    X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                    x11B.with(x11A.session());
                    x11B.msgId(x11A.msgId());
                    _Logger.info("unsubscribe topic:%s", x11A.getTopics());
                    return Collections.singletonList(Triple.of(x11B,
                                                               x11B.session(),
                                                               x11B.session()
                                                                   .getEncoder()));
                }
            }
            case 0x11E -> {
                X11E_QttDisconnect x11E = (X11E_QttDisconnect) consensusBody;
                ISession session = x11E.session();
                if(session != null) {
                    long origin = session.getIndex();
                    clean(origin);
                    x11E.session()
                        .innerClose();
                    _Logger.debug("device %#x → offline", origin);
                }
            }
            case 0x11F -> {

            }
        }
        return null;
    }

    @Override
    public <P extends IRoutable & IProtocol & IQoS> void register(long msgId, P body)
    {
        _StateService.add(msgId, body);
    }

    @Override
    public boolean ack(long msgId, long origin)
    {
        return _StateService.drop(origin, msgId);
    }

    @Override
    public void clean(long session)
    {
        _StateService.onDismiss(session);
    }

    @Override
    public void retain(String topic, IProtocol content)
    {
        if(_StateService.mappings()
                        .values()
                        .parallelStream()
                        .filter(subscribe->subscribe.pattern()
                                                    .asMatchPredicate()
                                                    .test(topic))
                        .peek(subscribe->subscribe.setRetain(content))
                        .count() == 0)
        {
            Pattern pattern = _QttTopicToRegex(topic);
            Subscribe subscribe = new Subscribe(pattern);
            subscribe.setRetain(content);
            _StateService.mappings()
                         .put(pattern, subscribe);
        }
    }

    @Override
    public List<Subscribe.Mapped> broker(String topic)
    {
        return _StateService.mappings()
                            .entrySet()
                            .parallelStream()
                            .filter(entry->entry.getKey()
                                                .asMatchPredicate()
                                                .test(topic))
                            .map(Map.Entry::getValue)
                            .flatMap(Subscribe::stream)
                            .collect(Collectors.toList());
    }

    public List<IThread.Topic> groupBy(long session)
    {
        _Logger.debug("group by :%#x", session);
        return _StateService.mappings()
                            .values()
                            .parallelStream()
                            .filter(subscribe->subscribe.contains(session))
                            .map(subscribe->new IThread.Topic(subscribe.pattern(), subscribe.level(session), 0))
                            .collect(Collectors.toList());
    }

    @Override
    public IThread.Subscribe subscribe(IThread.Topic topic, long session)
    {
        return _StateService.onSubscribe(topic, session);
    }

    @Override
    public void unsubscribe(IThread.Topic topic, long session)
    {
        _StateService.onUnsubscribe(topic, session);
    }

    public static Pattern _QttTopicToRegex(String topic)
    {
        _Logger.debug("topic source: [%s]", topic);
        topic = topic.replaceAll("\\++", "+");
        topic = topic.replaceAll("#+", "#");
        topic = topic.replaceAll("(/#)+", "/#");

        if(Pattern.compile("#\\+|\\+#")
                  .asPredicate()
                  .test(topic))
        {
            throw new IllegalArgumentException("topic error " + topic);
        }
        if(!Pattern.compile("(/\\+)$")
                   .asPredicate()
                   .test(topic))
        {
            topic = topic.replaceAll("(\\+)$", "");
        }
        topic = topic.replaceAll("^\\+", "([^\\$/]+)");
        topic = topic.replaceAll("(/\\+)$", "(/?[^/]*)");
        topic = topic.replaceAll("/\\+", "/([^/]+)");
        topic = topic.replaceAll("^#", "([^\\$]*)");
        topic = topic.replaceAll("^/#", "(/.*)");
        topic = topic.replaceAll("/#", "(/?.*)");
        return Pattern.compile(topic);
    }

    private void brokerTopic(IManager manager, X113_QttPublish x113, List<Subscribe.Mapped> routes, List<ITriple> results)
    {
        routes.forEach(mapped->{
            ISession session = manager.findSessionByIndex(mapped.session());
            if(session != null) {
                X113_QttPublish n113 = x113.copy();
                n113.with(session);
                n113.target(session.getIndex());
                n113.setLevel(mapped.level());
                if(mapped.level()
                         .getValue() > 0)
                {
                    n113.msgId(_MessageService.generateId(mapped.session()));
                    register(n113.msgId(), n113);
                }
                results.add(Triple.of(n113, session, session.getEncoder()));
            }
            else {
                _Logger.warning("no target session id[ %#x ] found ,ignore publish", mapped.session());
            }
        });
        _Logger.debug("broker[%s]→%s | %s", x113.getTopic(), routes, x113.toString());
    }

}
