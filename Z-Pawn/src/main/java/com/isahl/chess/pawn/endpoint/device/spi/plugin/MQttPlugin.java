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
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11F_QttAuth;
import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttTopicAlias;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X0D_Error;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttType.*;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.pawn.endpoint.device.config.MqttConfig;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.db.central.model.ZChatEntity;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider.AuthContext;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider.AuthResult;

import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.NULL;
import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.SINGLE;
import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
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
    private final MqttConfig     _MqttConfig;

    private final Map<Long, AuthContext> _AuthContexts = new ConcurrentHashMap<>();
    private final Map<String, IQttAuthProvider> _AuthProviders = new ConcurrentHashMap<>();

    private final Map<String, CachedDevice> _DeviceCache = new ConcurrentHashMap<>();
    private static final long DEVICE_CACHE_TTL_MS = 60000; // 1 minute TTL

    private static class CachedDevice {
        final DeviceEntity device;
        final long cachedAt;

        CachedDevice(DeviceEntity device) {
            this.device = device;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > DEVICE_CACHE_TTL_MS;
        }
    }

    @Autowired
    public MQttPlugin(IDeviceService deviceService, IMessageService messageService, IStateService stateService, MqttConfig mqttConfig)
    {
        _DeviceService = deviceService;
        _MessageService = messageService;
        _StateService = stateService;
        _MqttConfig = mqttConfig;
    }

    public void registerAuthProvider(IQttAuthProvider provider)
    {
        _AuthProviders.put(provider.getAuthMethod(), provider);
        _Logger.info("Registered auth provider: %s", provider.getAuthMethod());
    }

    private DeviceEntity getDeviceByToken(String token)
    {
        CachedDevice cached = _DeviceCache.get(token);
        if(cached != null && !cached.isExpired()) {
            _Logger.debug("Device cache hit for token: %s", token);
            return cached.device;
        }

        DeviceEntity device = _DeviceService.findByToken(token);
        if(device != null) {
            _DeviceCache.put(token, new CachedDevice(device));
            _Logger.debug("Device cached for token: %s", token);
        }
        else {
            _DeviceCache.remove(token);
        }

        return device;
    }

    private void invalidateDeviceCache(String token)
    {
        _DeviceCache.remove(token);
    }

    private long handleV5Connection(X111_QttConnect x111, X112_QttConnack x112, ISession session)
    {
        _Logger.debug("Handling MQTT v5 connection from: %s", x111.getClientId());

        DeviceEntity device = getDeviceByToken(x111.getClientId());
        if(device == null) {
            x112.rejectIdentifier();
            return ZUID.INVALID_PEER_ID;
        }

        if(!device.getUsername().equalsIgnoreCase(x111.getUserName()) ||
           !CryptoUtil.constantTimeEquals(device.getPassword(), x111.getPassword())) {
            x112.rejectNotAuthorized();
            return ZUID.INVALID_PEER_ID;
        }

        long deviceId = device.primaryKey();

        long sessionExpiry = x111.getSessionExpiryInterval();
        long effectiveExpiry = _MqttConfig.getEffectiveSessionExpiryInterval(sessionExpiry);
        x112.setSessionExpiryInterval(effectiveExpiry);

        int clientReceiveMax = x111.getReceiveMaximum();
        int serverReceiveMax = _MqttConfig.getServerReceiveMaximum();
        x112.setReceiveMaximum(serverReceiveMax);

        int clientTopicAliasMax = x111.getTopicAliasMaximum();
        int effectiveTopicAliasMax = _MqttConfig.getEffectiveTopicAliasMaximum(clientTopicAliasMax);
        x112.setTopicAliasMaximum(effectiveTopicAliasMax);

        int clientMaxPacketSize = x111.getMaximumPacketSize();
        int serverMaxPacketSize = _MqttConfig.getMaximumPacketSize();
        if(clientMaxPacketSize > 0 && clientMaxPacketSize < serverMaxPacketSize) {
            x112.setMaximumPacketSize(clientMaxPacketSize);
        }
        else {
            x112.setMaximumPacketSize(serverMaxPacketSize);
        }

        x112.setMaximumQoS(_MqttConfig.getMaximumQoS());

        x112.setRetainAvailable(_MqttConfig.isRetainAvailable());
        x112.setWildcardSubscriptionAvailable(_MqttConfig.isWildcardSubscriptionAvailable());
        x112.setSubscriptionIdentifierAvailable(_MqttConfig.isSubscriptionIdentifierAvailable());
        x112.setSharedSubscriptionAvailable(_MqttConfig.isSharedSubscriptionAvailable());

        if(_MqttConfig.isEnhancedAuthEnabled()) {
            String authMethod = x111.getAuthenticationMethod();
            if(authMethod != null && _MqttConfig.isAuthMethodSupported(authMethod)) {
                _Logger.debug("Enhanced auth requested: %s", authMethod);

                if(x111.getAuthenticationData() != null) {
                    AuthContext authContext = new AuthContext(session.index(), x111.getClientId(), authMethod);
                    IQttAuthProvider.AuthResult result = handleContinueAuth(authMethod, x111.getAuthenticationData(), authContext);
                    if(result.isSuccess()) {
                        x112.setAuthenticationMethod(authMethod);
                        if(result.getAuthData() != null) {
                            x112.setAuthenticationData(result.getAuthData());
                        }
                        _Logger.info("Enhanced authentication success for method: %s", authMethod);
                    }
                    else if(result.isContinue()) {
                        x112.setAuthenticationMethod(authMethod);
                        x112.setAuthenticationData(result.getAuthData());
                        AuthContext ctx = new AuthContext(session.index(), x111.getClientId(), authMethod);
                        _AuthContexts.put(session.index(), ctx);
                        x112.rejectBadAuthenticationMethod();
                        _Logger.info("Enhanced authentication continue for method: %s", authMethod);
                        return ZUID.INVALID_PEER_ID;
                    }
                    else {
                        _Logger.warning("Enhanced authentication failed for method: %s, reason: %s", authMethod, result.getReason());
                        x112.rejectNotAuthorized();
                        return ZUID.INVALID_PEER_ID;
                    }
                }
                else {
                    x112.setAuthenticationMethod(authMethod);
                    _Logger.info("Enhanced authentication accepted for method: %s", authMethod);
                }
            }
            else if(authMethod != null) {
                _Logger.warning("Unsupported authentication method: %s", authMethod);
                x112.rejectNotAuthorized();
                return ZUID.INVALID_PEER_ID;
            }
        }

        if(x111.isRequestResponseInformation()) {
            x112.setResponseInformation("response_info_available");
        }

        QttContext context = session.getContext(QttContext.class);
        if(context != null) {
            context.initV5Context(x112.getProperties());

            if(clientReceiveMax > 0) {
                context.setInboundQuota(clientReceiveMax);
            }

            if(effectiveTopicAliasMax > 0) {
                context.setTopicAliasManager(new QttTopicAlias(effectiveTopicAliasMax));
            }
        }

        x112.responseOk();
        _Logger.info("MQTT v5 connection accepted: client=%s, sessionExpiry=%d, recvMax=%d, topicAliasMax=%d",
            x111.getClientId(), effectiveExpiry, serverReceiveMax, effectiveTopicAliasMax);

        return deviceId;
    }

    private IQttAuthProvider.AuthResult handleContinueAuth(String authMethod, byte[] authData, AuthContext context)
    {
        IQttAuthProvider provider = _AuthProviders.get(authMethod);
        if(provider == null) {
            _Logger.warning("No auth provider found for method: %s", authMethod);
            return IQttAuthProvider.AuthResult.failure("Unsupported authentication method");
        }

        return provider.continueAuth(authMethod, authData, context);
    }

    private IQttAuthProvider.AuthResult handleAuth(String authMethod, byte[] authData, long sessionId)
    {
        AuthContext context = _AuthContexts.get(sessionId);
        if(context == null) {
            _Logger.warning("No auth context found for session: %d", sessionId);
            return IQttAuthProvider.AuthResult.failure("No authentication in progress");
        }

        IQttAuthProvider.AuthResult result = handleContinueAuth(authMethod, authData, context);
        if(result.isSuccess() || result.isFailure()) {
            _AuthContexts.remove(sessionId);
        }
        return result;
    }

    @Override
    public boolean isSupported(IoSerial input)
    {
        int serial = input.serial();
        return serial == serialOf(CONNECT) ||
               serial == serialOf(CONNACK) ||
               serial == serialOf(PUBLISH) ||
               serial == serialOf(PUBACK) ||
               serial == serialOf(PUBREC) ||
               serial == serialOf(PUBREL) ||
               serial == serialOf(PUBCOMP) ||
               serial == serialOf(SUBSCRIBE) ||
               serial == serialOf(SUBACK) ||
               serial == serialOf(UNSUBSCRIBE) ||
               serial == serialOf(UNSUBACK) ||
               serial == serialOf(PINGREQ) ||
               serial == serialOf(PINGRESP) ||
               serial == serialOf(DISCONNECT) ||
               serial == serialOf(AUTH) ||
               input instanceof ZChatEntity;
    }

    public boolean isSupported(ISession session)
    {
        return session.getFactory() == QttFactory._Instance;
    }

    @Override
    public void onExchange(IProtocol body, List<ITriple> load)
    {
        _Logger.debug("onExchange:%s ", body);
        if(body.serial() == serialOf(PUBLISH)) {
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
                //client::x113 → server::x115 → client::x116 → server , 服务端收到 x116,需要注意
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
                            n113.withTopic(received.getTopic());
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
            case 0x11C -> {
                load.add(Triple.of(new X11D_QttPingresp().with(session), session, session.encoder()));
                _Logger.debug("session: %#x receive heartbeat", session.index());
            }
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

                if(x111.getVersion() == VERSION_V5_0 && _MqttConfig.isEnabled()) {
                    long deviceId = handleV5Connection(x111, x112, session);
                    if(deviceId == ZUID.INVALID_PEER_ID) {
                        return Triple.of(x112, null, SINGLE);
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
                        return Triple.of(x112, null, SINGLE);
                    }
                }
                else {
                    long deviceId = ZUID.INVALID_PEER_ID;
                    if(x112.isOk()) {
                        _Logger.info("Login Client: %s", x111.getClientId());
                        DeviceEntity device = getDeviceByToken(x111.getClientId());
                        if(device == null) {
                            x112.rejectIdentifier();
                        }
                        else if(!device.getUsername()
                                      .equalsIgnoreCase(x111.getUserName()) ||
                                !CryptoUtil.constantTimeEquals(device.getPassword(), x111.getPassword()))
                        {
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
                X11F_QttAuth x11F = (X11F_QttAuth) input;
                String authMethod = x11F.getAuthMethod();
                byte[] authData = x11F.getAuthData();

                if(x11F.isReauthenticate()) {
                    _Logger.info("Re-authentication request from session: %d", session.index());
                }

                IQttAuthProvider.AuthResult result = handleAuth(authMethod, authData, session.index());
                X11F_QttAuth authResponse = new X11F_QttAuth();

                if(result.isSuccess()) {
                    authResponse.setSuccess();
                    if(result.getAuthData() != null) {
                        authResponse.setAuthData(result.getAuthData());
                    }
                    _Logger.info("Authentication success for session: %d", session.index());
                }
                else if(result.isContinue()) {
                    authResponse.setContinueAuthentication();
                    authResponse.setAuthData(result.getAuthData());
                    _Logger.info("Authentication continue for session: %d", session.index());
                }
                else {
                    authResponse.setReasonCode(CodeMqtt.REJECT_NOT_AUTHORIZED.getCode());
                    _Logger.warning("Authentication failed for session: %d, reason: %s", session.index(), result.getReason());
                }

                return Triple.of(authResponse, session, session.encoder());
            }
            default -> {
                return null;
            }
        }
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
        _Logger.debug("MsgDeliveryStatus: msgId=%d PENDING->SENDING", msgId);
    }

    @Override
    public boolean ack(long msgId, long origin)
    {
        boolean result = _StateService.drop(origin, msgId);
        if(result) {
            _Logger.debug("MsgDeliveryStatus: msgId=%d SENDING->DELIVERED", msgId);
        }
        return result;
    }

    @Override
    public void clean(long session)
    {
        IProtocol willMessage = _StateService.dismiss(session);
        if(willMessage != null) {
            _Logger.info("Publishing will message for session: %d", session);
            onLogic(null, null, willMessage, Collections.emptyList());
        }
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
        ZChatEntity zchatEntity = (ZChatEntity) request;
        _MessageService.stateInit(zchatEntity);
        broker(zchatEntity.getTopic()).forEach(mapped->{
            long target = mapped.session();
            X113_QttPublish n113 = new X113_QttPublish().withTopic(zchatEntity.getTopic());
            n113.withSub(zchatEntity.getMessage());
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
