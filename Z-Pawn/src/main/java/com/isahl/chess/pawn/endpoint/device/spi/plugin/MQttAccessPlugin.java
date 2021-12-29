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
import com.isahl.chess.bishop.protocol.mqtt.ctrl.QttControl;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11D_QttPingresp;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.data.DeviceSubscribe;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttRouter;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttStorage;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X0A_Shutdown;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.pawn.endpoint.device.api.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.queen.io.core.features.model.content.ICommand;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.SINGLE;
import static com.isahl.chess.king.config.KingCode.SUCCESS;

/**
 * @author william.d.zk
 */
@Component
public class MQttAccessPlugin
        implements IAccessService,
                   IQttRouter,
                   IValid
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getName());

    private final IDeviceService                       _DeviceService;
    private final IQttStorage                          _QttStorage;
    /*=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=*/
    /**
     * 在 link-consumer 中处理 subscribe 和 unsubscribe
     * 单线程执行，使用TreeMap 操作
     */
    private final Map<Pattern, Subscribe>              _Topic2SessionsMap = new TreeMap<>(Comparator.comparing(Pattern::pattern));
    private final Map<Long, Map<Integer, ICommand<?>>> _QttIdentifierMap  = new ConcurrentSkipListMap<>();
    private final Queue<Pair<Long, Instant>>           _SessionIdleQueue  = new ConcurrentLinkedQueue<>();

    @Autowired
    public MQttAccessPlugin(IDeviceService deviceService, IQttStorage qttStorage)
    {
        _DeviceService = deviceService;
        _QttStorage = qttStorage;
    }

    @Override
    public boolean isSupported(IoSerial input)
    {
        return input.serial() >= 0x111 && input.serial() <= 0x11F;
    }

    @Override
    public List<ITriple> logicHandle(IManager manager, ISession session, IProtocol content)
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
                        x114.setMsgId(x113.getMsgId());
                        x114.with(session);
                        results.add(Triple.of(x114, session, session.getEncoder()));
                    case ALMOST_ONCE:
                        brokerTopic(manager, x113, broker(x113.getTopic()), results);
                        break;
                    case EXACTLY_ONCE:
                        X115_QttPubrec x115 = new X115_QttPubrec();
                        x115.setMsgId(x113.getMsgId());
                        x115.with(session);
                        results.add(Triple.of(x115, session, session.getEncoder()));
                        register(x115, session.getIndex());
                        _QttStorage.receivedStorage((int) x113.getMsgId(),
                                                    x113.getTopic(),
                                                    x113.payload(),
                                                    session.getIndex());
                        break;
                    default:
                        break;
                }
                break;
            case 0x114:
                //x113.QoS1 → client → x114, 服务端不存储需要client持有的消息
                X114_QttPuback x114 = (X114_QttPuback) content;
                ack(x114, session.getIndex());
                _QttStorage.deleteMessage((int) x114.getMsgId(), session.getIndex());
                break;
            case 0x115:
                //x113.QoS2 → client → x115, 服务端恒定返回x116,Router无需操作。
                X115_QttPubrec x115 = (X115_QttPubrec) content;
                X116_QttPubrel x116 = new X116_QttPubrel();
                x116.setMsgId(x115.getMsgId());
                results = new LinkedList<>();
                results.add(Triple.of(x116.with(session), session, session.getEncoder()));
                register(x116, session.getIndex());
                _QttStorage.deleteMessage((int) x115.getMsgId(), session.getIndex());
                break;
            case 0x116:
                //client → x113 → server → x115 → client → x116 → server , 服务端收到 x116,需要注意
                x116 = (X116_QttPubrel) content;
                X117_QttPubcomp x117 = new X117_QttPubcomp();
                x117.setMsgId(x116.getMsgId());
                results = new LinkedList<>();
                results.add(Triple.of(x117.with(session), session, session.getEncoder()));
                if(ack(x116, session.getIndex())) {
                    if(_QttStorage.hasReceived((int) x116.getMsgId(), session.getIndex())) {
                        if((x113 = _QttStorage.takeStorage((int) x116.getMsgId(), session.getIndex())) != null) {
                            brokerTopic(manager, x113, broker(x113.getTopic()), results);
                        }
                    }
                }
                break;
            case 0x117:
                x117 = (X117_QttPubcomp) content;
                ack(x117, session.getIndex());
                break;
            case 0x11C:
                return Collections.singletonList(Triple.of(new X11D_QttPingresp().with(session),
                                                           session,
                                                           session.getEncoder()));
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
                        X0A_Shutdown x108 = new X0A_Shutdown();
                        x108.with(old);
                        _Logger.info("re-login ok %s, wait for consistent notify", x111.getClientId());
                        return new Triple<>(x108, x111, SINGLE);
                    }
                    else {
                        _Logger.info("login check ok:%s, wait for consistent notify", x111.getClientId());
                        return new Triple<>(null, x111, SINGLE);
                    }
                }
                else {
                    _Logger.info("reject %s",
                                 x112.getCode()
                                     .name());
                    return new Triple<>(x112, null, SINGLE);
                }
            }
            case 0x118, 0x11A, 0x11E -> {
                _Logger.info("link control [%s] ", input);
                return new Triple<>(null, input, SINGLE);
            }
            case 0x11F -> {

            }
        }
        return null;
    }

    @Override
    public void onOffline(ISession session)
    {

    }

    @Override
    public List<ITriple> onConsistencyResult(IManager manager, long origin, IoSerial consensusBody)
    {
        ISession session = manager.findSessionByIndex(origin);
        switch(consensusBody.serial()) {
            case 0x111 -> {
                X111_QttConnect x111 = (X111_QttConnect) consensusBody;
                if(x111.rxCode() == SUCCESS) {
                    _Logger.info("%s login ok -> %#x", x111.getClientId(), origin);
                    if(x111.isClean()) {
                        clean(origin);
                    }
                    _QttStorage.sessionOnLogin(origin, this, x111);
                }
                if(session != null) {
                    X112_QttConnack x112 = new X112_QttConnack();
                    x112.with(session);
                    if(x111.rxCode() == SUCCESS) {
                        x112.responseOk();
                    }
                    else {
                        x112.rejectServerUnavailable();
                    }
                    return Collections.singletonList(new Triple<>(x112, session, session.getEncoder()));
                }
            }
            case 0x118 -> {
                X118_QttSubscribe x118 = (X118_QttSubscribe) consensusBody;
                Map<String, IQoS.Level> subscribes = x118.getSubscribes();
                if(subscribes != null && x118.rxCode() == SUCCESS) {
                    subscribes.forEach((topic, level)->{
                        Subscribe subscribe = subscribe(topic, level, origin);
                        if(subscribe != null) {
                            //TODO 统计单指令多个Subscribe的情况
                            _QttStorage.sessionOnSubscribe(origin, topic, level);
                        }
                    });
                    if(session != null) {
                        X119_QttSuback x119 = new X119_QttSuback();
                        x119.with(session);
                        x119.setMsgId(x118.getMsgId());
                        _Logger.info("subscribe topic:%s", x118.getSubscribes());
                        return Collections.singletonList(new Triple<>(x119, session, session.getEncoder()));
                    }
                }
            }
            case 0x11A -> {
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) consensusBody;
                List<String> topics = x11A.getTopics();
                if(topics != null && x11A.rxCode() == SUCCESS) {
                    topics.forEach(topic->{
                        unsubscribe(topic, origin);
                        _QttStorage.sessionOnUnsubscribe(origin, topic);
                    });
                    if(session != null) {
                        X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                        x11B.with(session);
                        x11B.setMsgId(x11A.getMsgId());
                        _Logger.info("unsubscribe topic:%s", x11A.getTopics());
                        return Collections.singletonList(new Triple<>(x11B, session, session.getEncoder()));
                    }
                }
            }
            case 0x11E -> {
                if(session != null) {
                    clean(session.getIndex());
                    session.innerClose();
                    _Logger.debug("device %#x → offline", origin);
                }
                else {
                    _Logger.info("disconnect :%#x,session → null", origin);
                }
            }
            case 0x11F -> {

            }
        }
        return null;
    }

    @Override
    public void register(ICommand<?> stateMessage, long session)
    {
        int msgId = (int) stateMessage.getMsgId();
        if(_QttIdentifierMap.computeIfPresent(session, (key, _MsgIdMessageMap)->{
            IControl<?> old = _MsgIdMessageMap.put(msgId, stateMessage);
            if(old == null) {
                _Logger.debug("retry receive: %s", stateMessage);
            }
            return _MsgIdMessageMap;
        }) == null)
        {
            //previous == null
            final Map<Integer, ICommand<?>> _LocalIdMessageMap = new HashMap<>(16);
            _LocalIdMessageMap.put(msgId, stateMessage);
            _QttIdentifierMap.put(session, _LocalIdMessageMap);
            _Logger.debug("first receive: %s", stateMessage);
        }
    }

    @Override
    public boolean ack(ICommand<?> stateMessage, long session)
    {
        int idleMax = stateMessage.session()
                                  .getReadTimeOutSeconds();
        int msgId = (int) stateMessage.getMsgId();
        boolean[] ack = { true, true
        };
        if(ack[0] = _QttIdentifierMap.computeIfPresent(session, (key, old)->{
            _Logger.debug("ack %d @ %#x", msgId, session);
            ack[1] = old.remove(msgId) != null;
            return old.isEmpty() ? old : null;
        }) != null)
        {
            _Logger.debug("idle session: %#x", session);
            _SessionIdleQueue.offer(new Pair<>(session, Instant.now()));
        }
        for(Iterator<Pair<Long, Instant>> it = _SessionIdleQueue.iterator(); it.hasNext(); ) {
            IPair pair = it.next();
            long rmSessionIndex = pair.getFirst();
            Instant idle = pair.getSecond();
            if(Instant.now()
                      .isAfter(idle.plusSeconds(idleMax)))
            {
                _QttIdentifierMap.remove(rmSessionIndex);
                it.remove();
            }
        }
        return ack[0] & ack[1];
    }

    @Override
    public void clean(long session)
    {
        Optional.ofNullable(_QttIdentifierMap.remove(session))
                .ifPresent(Map::clear);
        _Topic2SessionsMap.values()
                          .forEach(map->map.remove(session));
        _QttStorage.cleanSession(session);
    }

    @Override
    public void retain(String topic, QttControl msg)
    {
        Pattern pattern = topicToRegex(topic);
        if(msg.payload() == null) {
            _Topic2SessionsMap.computeIfPresent(pattern, (k, v)->{
                v.mRetained = null;
                return v;
            });
        }
        else {
            _Topic2SessionsMap.computeIfAbsent(pattern, Subscribe::new).mRetained = msg;
        }
    }

    @Override
    public Map<Long, IQoS.Level> broker(String topic)
    {
        return _Topic2SessionsMap.entrySet()
                                 .parallelStream()
                                 .map(entry->{
                                     Pattern pattern = entry.getKey();
                                     Subscribe subscribe = entry.getValue();
                                     return pattern.matcher(topic)
                                                   .matches() && !subscribe._SessionMap.isEmpty()
                                            ? subscribe._SessionMap : null;

                                 })
                                 .filter(Objects::nonNull)
                                 .map(Map::entrySet)
                                 .flatMap(Set::stream)
                                 .collect(Collectors.toMap(Map.Entry::getKey,
                                                           Map.Entry::getValue,
                                                           (l, r)->l.getValue() > r.getValue() ? l : r,
                                                           ConcurrentSkipListMap::new));
    }

    public DeviceSubscribe groupBy(long session)
    {
        _Logger.debug("group by :%#x", session);
        return new DeviceSubscribe(_Topic2SessionsMap.entrySet()
                                                     .parallelStream()
                                                     .flatMap(entry->{
                                                         Pattern pattern = entry.getKey();
                                                         Subscribe subscribe = entry.getValue();
                                                         Map<Long, IQoS.Level> sessionsLv = subscribe._SessionMap;
                                                         return sessionsLv.entrySet()
                                                                          .stream()
                                                                          .filter(e->e.getKey() == session)
                                                                          .map(e->new Pair<>(pattern.pattern(),
                                                                                             e.getValue()));
                                                     })
                                                     .collect(Collectors.toMap(IPair::getFirst, IPair::getSecond)));
    }

    @Override
    public Subscribe subscribe(String topic, IQoS.Level level, long session)
    {
        try {
            Pattern pattern = topicToRegex(topic);
            _Logger.debug("topic %s,pattern %s", topic, pattern);
            Subscribe subscribe = _Topic2SessionsMap.computeIfAbsent(pattern, p->new Subscribe(pattern));
            if(session != 0) {
                IQoS.Level lv = subscribe._SessionMap.computeIfPresent(session,
                                                                       (key, old)->old.getValue() > level.getValue()
                                                                                   ? old : level);
                if(lv == null) {
                    subscribe._SessionMap.put(session, level);
                }
            }
            return subscribe;
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
            _Logger.warning("subscribe topic %s pattern error:", topic);
        }
        return null;
    }

    @Override
    public void unsubscribe(String topic, long session)
    {
        _Topic2SessionsMap.forEach((pattern, subscribe)->{
            Matcher matcher = pattern.matcher(topic);
            if(matcher.matches()) {
                subscribe.remove(session);
            }
        });
    }

    private Pattern topicToRegex(String topic)
    {
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

    private void brokerTopic(IManager manager,
                             X113_QttPublish x113,
                             Map<Long, IQoS.Level> routes,
                             List<ITriple> results)
    {
        routes.forEach((kIdx, lv)->{
            ISession session = manager.findSessionByIndex(kIdx);
            if(session != null) {
                X113_QttPublish n113 = x113.copy();
                n113.with(session);
                n113.setLevel(lv);
                if(lv.getValue() > 0) {
                    n113.setMsgId(_QttStorage.generateMsgId(kIdx));
                    register(n113, kIdx);
                }
                results.add(Triple.of(n113, session, session.getEncoder()));
                _QttStorage.brokerStorage((int) n113.getMsgId(), n113.getTopic(), n113.payload(), kIdx);
            }
        });

        _Logger.debug("broker[%s]→%s | %s",
                      x113.getTopic(),
                      IoUtil.longArrayToHex(routes.keySet()
                                                  .toArray(new Long[0])),
                      x113.toString());
    }

}
