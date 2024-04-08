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
import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X0D_Error;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.db.central.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.resource.features.IMessageService;
import com.isahl.chess.pawn.endpoint.device.resource.features.IStateService;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IRoutable;
import com.isahl.chess.queen.io.core.features.model.routes.IRouter;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;
import com.isahl.chess.queen.io.core.features.model.session.IExchanger;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.NULL;
import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.SINGLE;
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
        return (input.serial() >= 0x111 && input.serial() <= 0x11F) || input instanceof MessageEntity;
    }

    public boolean isSupported(ISession session)
    {
        return session.getFactory() == QttFactory._Instance;
    }

    @Override
    public void onExchange(IProtocol body, List<ITriple> load)
    {
        _Logger.debug("onExchange:%s ", body);
        if(body.serial() == 0x113) {
            X113_QttPublish x113 = (X113_QttPublish) body;
            if(x113.level()
                   .getValue() > 0)
            {
                register(x113.msgId(), x113);
            }
            load.add(Triple.of(x113,
                               x113.session(),
                               x113.session()
                                   .encoder()));
        }
        else {
            _Logger.warning("unsupported message type: " + body);
        }
    }

    @Override
    public void onLogic(IExchanger exchanger, ISession session, IProtocol content, List<ITriple> load)
    {
        switch(content.serial()) {
            case 0x113 -> {
                X113_QttPublish x113 = (X113_QttPublish) content;
                if(x113.isRetain()) {
                    retain(x113.topic(), x113);
                }
                switch(x113.level()) {
                    case AT_LEAST_ONCE:
                        X114_QttPuback x114 = new X114_QttPuback();
                        x114.msgId(x113.msgId());
                        x114.with(session);
                        load.add(Triple.of(x114, session, session.encoder()));
                    case ALMOST_ONCE:
                        brokerTopic(exchanger, x113, broker(x113.topic()), load);
                        break;
                    case EXACTLY_ONCE:
                        X115_QttPubrec x115 = new X115_QttPubrec();
                        x115.msgId(x113.msgId());
                        x115.with(session);
                        x115.target(session.index());
                        load.add(Triple.of(x115, session, session.encoder()));
                        register(x115.msgId(), x115);
                        _StateService.store(session.index(), x113.msgId(), x113);
                        break;
                    default:
                        break;
                }
            }
            case 0x114 -> {
                //x113.QoS1 → client → x114, 服务端不存储需要client持有的消息
                X114_QttPuback x114 = (X114_QttPuback) content;
                ack(x114.msgId(), session.index());
            }
            case 0x115 -> {
                //x113.QoS2 → client → x115, 服务端恒定返回x116,Router无需操作。
                X115_QttPubrec x115 = (X115_QttPubrec) content;
                X116_QttPubrel x116 = new X116_QttPubrel();
                x116.msgId(x115.msgId());
                x116.target(session.index());
                load.add(Triple.of(x116.with(session), session, session.encoder()));
                register(x116.msgId(), x116);
            }
            case 0x116 -> {
                //client → x113 → server → x115 → client → x116 → server , 服务端收到 x116,需要注意
                X116_QttPubrel x116 = (X116_QttPubrel) content;
                X117_QttPubcomp x117 = new X117_QttPubcomp();
                x117.msgId(x116.msgId());
                load.add(Triple.of(x117.with(session), session, session.encoder()));
                if(ack(x116.msgId(), session.index())) {
                    if(_StateService.exists(session.index(), x116.msgId())) {
                        MsgStateEntity received = _StateService.extract(session.index(), x116.msgId());
                        if(received != null) {
                            X113_QttPublish n113 = new X113_QttPublish();
                            n113.with(session);
                            n113.withTopic(received.topic());
                            n113.withSub(received.payload());
                            n113.setLevel(IQoS.Level.EXACTLY_ONCE);
                            brokerTopic(exchanger, n113, broker(n113.topic()), load);
                        }
                    }
                }
            }
            case 0x117 -> {
                X117_QttPubcomp x117 = (X117_QttPubcomp) content;
                ack(x117.msgId(), session.index());
            }
            case 0x11C -> load.add(Triple.of(new X11D_QttPingresp().with(session), session, session.encoder()));
        }
    }

    @Override
    public ITriple onLink(IManager manager, ISession session, IProtocol input)
    {
        switch(input.serial()) {
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
                    ISession old = manager.mapSession(session.prefix(deviceId), session);
                    if(old != null) {
                        _Logger.info("re-login ok, wait for consistent notify; client[%s],close old",
                                     x111.getClientId());
                        old.innerClose();
                    }
                    else {
                        _Logger.info("login check ok, wait for consistent notify; client[%s]", x111.getClientId());
                    }
                    return Triple.of(null, x111, NULL);
                }
                else {
                    _Logger.info("reject %s",
                                 x112.getCode()
                                     .name());
                    return Triple.of(x112, null, SINGLE);
                }
            }
            case 0x118, 0x11A -> {
                _Logger.info("link control [%s] ", input);
                return Triple.of(null, input, NULL);
            }
            case 0x11E -> {
                _Logger.debug("disconnect [%#x] ", session.index());
                return Triple.of(new X0D_Error(), input, SINGLE);
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
    public List<ITriple> onConsistency(IManager manager, IConsistency backload, IoSerial consensusBody)
    {
        int code = backload.code();
        long origin = backload.origin();
        long client = backload.client();
        ISession os = manager.findSessionByIndex(origin);
        ISession cs = manager.fairLoadSessionByPrefix(client);
        switch(consensusBody.serial()) {
            case 0x111 -> {
                X111_QttConnect x111 = (X111_QttConnect) consensusBody;
                X112_QttConnack x112 = new X112_QttConnack();
                if(os != null && cs != null) {
                    // origin 在cs 上执行了登录
                    clean(origin);
                    manager.route(origin, client);
                    _Logger.debug("origin[%#x] login @[%#x], shutdown old", origin, client);
                    os.innerClose();
                }
                else if(os == null && cs != null) {
                    manager.route(origin, client);
                    _Logger.debug("origin[%#x] login @[%#x]", origin, client);
                }
                else if(os != null) { // cs == null 请求发生在本节点
                    x112.with(os);//为 112 关联 QttContext, context 是绑定在session上
                    if(code == SUCCESS) {
                        _Logger.info("%s → %#x login @[%#x]", x111.getClientId(), origin, client);
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
                    return Collections.singletonList(Triple.of(x112, os, os.encoder()));
                }
            }
            case 0x118 -> {
                X118_QttSubscribe x118 = (X118_QttSubscribe) consensusBody;
                Map<String, IQoS.Level> subscribes = x118.getSubscribes();
                if(code == SUCCESS) {
                    X119_QttSuback x119 = new X119_QttSuback();
                    x119.msgId(x118.msgId());
                    if(os != null) {
                        if(subscribes != null) {
                            _Logger.info("subscribe topic: %s", x118.getSubscribes());
                            _Logger.debug(" → origin[%#x] subscribe @[%#x], ack ", origin, client);
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
                        }
                        return Collections.singletonList(Triple.of(x119.with(os), os, os.encoder()));
                    }
                    else if(subscribes != null) {
                        _Logger.info("subscribe topic:%s", x118.getSubscribes());
                        _Logger.debug("origin[%#x] subscribe @[%#x]", origin, client);
                        subscribes.forEach((topic, level)->{
                            Topic t = new Topic(_QttTopicToRegex(topic), level, 0);
                            subscribe(t, origin);
                        });
                    }
                }
            }
            case 0x11A -> {
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) consensusBody;
                List<String> topics = x11A.getTopics();
                if(code == SUCCESS) {
                    X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                    x11B.msgId(x11A.msgId());
                    if(topics != null) {
                        _Logger.info("unsubscribe topic:%s", x11A.getTopics());
                        topics.forEach(topic->unsubscribe(new Topic(_QttTopicToRegex(topic)), origin));
                        _Logger.debug("origin[%#x] unsubscribe @[%#x]", origin, client);
                    }
                    if(os != null) {
                        return Collections.singletonList(Triple.of(x11B.with(os), os, os.encoder()));
                    }
                }
            }
            case 0x11E -> {
                // 集群同步的 disconnection 事件
                if(code == SUCCESS) {
                    _Logger.debug("origin [%#x]@[%#x] → offline", origin, client);
                    clean(origin);
                    ISession session = clean(manager, origin);
                    if(session != null) {
                        return Collections.singletonList(Triple.of(new X0D_Error(), session, null));
                    }
                    // else '连接' 已经 被清理了
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
        _StateService.dismiss(session);
    }

    public IProtocol onClose(ISession session)
    {
        return new X11E_QttDisconnect();
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

    private void brokerTopic(IExchanger exchanger,
                             X113_QttPublish x113,
                             List<Subscribe.Mapped> mappedList,
                             List<ITriple> results)
    {
        _Logger.debug("broker[%s]→%s | %s", x113.topic(), mappedList, x113.toString());
        mappedList.forEach(mapped->{
            long target = mapped.session();
            X113_QttPublish n113 = x113.duplicate();
            n113.target(target);
            n113.setLevel(mapped.level());
            if(mapped.level()
                     .getValue() > 0)
            {
                n113.msgId(_MessageService.generateId(target));
            }
            ISession session = exchanger.findSessionByIndex(target);
            if(session != null) {
                _Logger.debug("broker topic → session [%#x,%s] ", session.index(), mapped.level());
                n113.with(session);
                if(mapped.level()
                         .getValue() > 0)
                {
                    register(n113.msgId(), n113);
                }
                results.add(Triple.of(n113, session, session.encoder()));
            }
            else {
                exchanger.exchange(n113, target, QttFactory._Instance.serial(), results);
                _Logger.debug("no local routing,cluster exchange %#x", mapped.session());
            }
        });

    }

    @Override
    public void consume(IExchanger exchanger, IoSerial request, List<ITriple> results)
    {
        _Logger.debug("service consume\n\t%s \n→ broker ", request);
        MessageEntity messageEntity = (MessageEntity) request;
        broker(messageEntity.getTopic()).forEach(mapped->{
            long target = mapped.session();
            X113_QttPublish n113 = new X113_QttPublish().withTopic(messageEntity.getTopic());
            n113.withSub(messageEntity.getMessage());
            n113.target(target);
            if(mapped.level()
                     .getValue() > 0)
            {
                n113.msgId(_MessageService.generateId(target));
            }
            ISession session = exchanger.findSessionByIndex(target);
            if(session != null) {
                _Logger.debug("broker topic → session [%#x,%s] ", session.index(), mapped.level());
                n113.with(session);
                if(mapped.level()
                         .getValue() > 0)
                {
                    register(n113.msgId(), n113);
                }
                results.add(Triple.of(n113, session, session.encoder()));
            }
            else {
                exchanger.exchange(n113, target, QttFactory._Instance.serial(), results);
                _Logger.debug("no local routing, cluster exchange %#x", mapped.session());
            }
        });
    }
}
