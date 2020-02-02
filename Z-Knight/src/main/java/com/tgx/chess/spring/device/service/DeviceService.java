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

package com.tgx.chess.spring.device.service;

import static com.tgx.chess.bishop.io.Direction.OWNER_CLIENT;
import static com.tgx.chess.bishop.io.Direction.OWNER_SERVER;
import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.IServerConfig;
import com.tgx.chess.queen.config.ISocketConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.tgx.chess.ZApiExecption;
import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.biz.db.dao.MessageEntry;
import com.tgx.chess.bishop.biz.device.DeviceNode;
import com.tgx.chess.bishop.io.Direction;
import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.mqtt.control.X113_QttPublish;
import com.tgx.chess.bishop.io.mqtt.control.X114_QttPuback;
import com.tgx.chess.bishop.io.mqtt.control.X116_QttPubrel;
import com.tgx.chess.bishop.io.mqtt.handler.QttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X23_SignInResult;
import com.tgx.chess.bishop.io.zprotocol.device.X24_UpdateToken;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.spring.device.model.MessageBody;
import com.tgx.chess.spring.jpa.device.dao.DeviceEntity;
import com.tgx.chess.spring.jpa.device.dao.MessageEntity;
import com.tgx.chess.spring.jpa.device.repository.DeviceRepository;
import com.tgx.chess.spring.jpa.device.repository.MessageRepository;

/**
 * @author william.d.zk
 * @date 2019-06-10
 */
@PropertySource({ "classpath:device.ws.properties",
                  "classpath:device.qtt.properties",
                  "classpath:device.config.properties" })
@Service
public class DeviceService
        implements
        IRepository<IPair>
{
    private final Logger                                   _Logger                = Logger.getLogger(getClass().getName());
    private final CryptUtil                                _CryptUtil             = new CryptUtil();
    private final DeviceRepository                         _DeviceRepository;
    private final MessageRepository                        _MessageRepository;
    private final DeviceNode                               _DeviceNode;
    private final Map<Long,
                      Map<Long,
                          IControl<ZContext>>>             _DeviceMessageStateMap = new ConcurrentSkipListMap<>();

    @Value("${invalid.days}")
    private int    invalidDurationOfDays;
    @Value("${random.seed}")
    private String randomSeed;

    @Autowired
    DeviceService(@Value("${qtt.server.host}") String qttServiceHost,
                  @Value("${qtt.server.port}") int qttServicePort,
                  @Value("${ws.server.host}") String wsServiceHost,
                  @Value("${ws.server.port}") int wsServicePort,
                  DeviceRepository deviceRepository,
                  MessageRepository messageRepository,
                  IBizIoConfig bizIoConfig,
                  IServerConfig serverConfig)
    {
        _DeviceRepository = deviceRepository;
        _MessageRepository = messageRepository;
        List<ITriple> hosts = new ArrayList<>(2);
        hosts.add(new Triple<>(wsServiceHost, wsServicePort, ZSort.WS_SERVER));
        hosts.add(new Triple<>(qttServiceHost, qttServicePort, ZSort.QTT_SYMMETRY));
        _DeviceNode = new DeviceNode(hosts, this, new QttRouter(), bizIoConfig, serverConfig);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _DeviceNode.start();
    }

    public DeviceEntry convertDevice(DeviceEntity entity)
    {
        Objects.requireNonNull(entity);
        DeviceEntry deviceEntry = new DeviceEntry();
        deviceEntry.setPrimaryKey(entity.getId());
        deviceEntry.setToken(entity.getToken());
        deviceEntry.setInvalidTime(entity.getInvalidAt()
                                         .getTime());
        return deviceEntry;
    }

    private DeviceEntity findDevice(@NonNull DeviceEntity device)
    {
        return _DeviceRepository.findBySn(device.getSn());
    }

    public DeviceEntity saveDevice(@NonNull DeviceEntity device)
    {
        if (isBlank(device.getSn())) { throw new ZApiExecption("unique device info null"); }
        DeviceEntity exist = findDevice(device);
        if (Objects.nonNull(exist)) {
            device.setId(exist.getId());
        }
        else {
            String source = String.format("sn:%s,random %s%d",
                                          device.getSn(),
                                          randomSeed,
                                          Instant.now()
                                                 .toEpochMilli());
            _Logger.info("new device %s ", source);
            device.setToken(IoUtil.bin2Hex(_CryptUtil.sha256(source.getBytes(StandardCharsets.UTF_8))));
        }
        if (exist == null || isBlank(exist.getPassword())) {
            device.setPassword(_CryptUtil.randomPassword(5, 32));
            device.increasePasswordId();
            device.setInvalidAt(Date.from(Instant.now()
                                                 .plus(invalidDurationOfDays, ChronoUnit.DAYS)));
        }
        else if (!isBlank(device.getPassword()) && device.getPasswordId() > exist.getPasswordId()) {
            device.setInvalidAt(Date.from(Instant.now()
                                                 .plus(invalidDurationOfDays, ChronoUnit.DAYS)));
        }
        else {
            device.setPassword(exist.getPassword());
            device.setInvalidAt(exist.getInvalidAt());
        }
        return _DeviceRepository.save(device);
    }

    public void updateDevices(Collection<DeviceEntity> devices)
    {
        _DeviceRepository.saveAll(devices);
    }

    public DeviceEntity findDeviceByToken(String token)
    {
        return _DeviceRepository.findByToken(token);
    }

    public DeviceEntity findDeviceBySn(String sn)
    {
        return _DeviceRepository.findBySn(sn);
    }

    private DeviceEntry auth(DeviceEntity deviceEntity, String password)
    {
        DeviceEntry deviceEntry = new DeviceEntry();
        if (Objects.nonNull(deviceEntity)) {
            _Logger.info("device [%s] | connect ", deviceEntity.getSn());
            deviceEntry.setPrimaryKey(deviceEntity.getId());
            String origin = deviceEntity.getPassword();
            if (isBlank(origin)
                || "*".equals(origin)
                || ".".equals(origin)
                || (Objects.nonNull(password) && origin.equals(password)))
            {
                deviceEntry.setOperation(IStorage.Operation.OP_APPEND);
            }
            else {
                deviceEntry.setOperation(IStorage.Operation.OP_INVALID);
            }
            deviceEntry.setInvalidTime(deviceEntity.getInvalidAt()
                                                   .getTime());
        }
        else {
            deviceEntry.setOperation(IStorage.Operation.OP_NULL);
        }
        return deviceEntry;
    }

    @Override
    public IPair save(IProtocol target)
    {
        switch (target.serial())
        {
            case X20_SignUp.COMMAND:
                X20_SignUp x20 = (X20_SignUp) target;
                X21_SignUpResult x21 = new X21_SignUpResult();
                x21.setFailed();
                String deviceSn = x20.getSn();
                String devicePwd = x20.getPassword();
                long pwdId = x20.getPasswordId();
                DeviceEntity deviceEntity = new DeviceEntity();
                deviceEntity.setSn(deviceSn);
                deviceEntity.setPasswordId(pwdId);
                deviceEntity.setPassword(devicePwd);
                try {
                    DeviceEntity _Device = saveDevice(deviceEntity);
                    x21.setSuccess();
                    x21.setToken(IoUtil.hex2bin(_Device.getToken()));
                    x21.setPasswordId(_Device.getPasswordId());
                    return new Pair<>(convertDevice(deviceEntity), x21);
                }
                catch (Exception e) {
                    _Logger.warning("#save", e);
                }
                return new Pair<>(null, x21);
            case X24_UpdateToken.COMMAND:
                X24_UpdateToken x24 = (X24_UpdateToken) target;
                break;
        }
        return null;
    }

    @Override
    public IPair find(IProtocol key)
    {
        _Logger.debug("find %d", key.serial());
        String deviceToken = null, password = null;
        switch (key.serial())
        {
            case X22_SignIn.COMMAND:
                X22_SignIn x22 = (X22_SignIn) key;
                deviceToken = IoUtil.bin2Hex(x22.getToken());
                password = x22.getPassword();
                X23_SignInResult x23 = new X23_SignInResult();
                DeviceEntity deviceEntity = findDeviceByToken(Objects.requireNonNull(deviceToken));
                DeviceEntry deviceEntry = auth(deviceEntity, password);
                switch (deviceEntry.getOperation())
                {
                    case OP_NULL:
                    case OP_INVALID:
                        x23.setFailed();
                        break;
                    case OP_APPEND:
                        x23.setSuccess();
                        x23.setInvalidTime(deviceEntry.getInvalidTime());
                        break;
                }
                return new Pair<>(deviceEntry, x23);
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) key;
                deviceToken = x111.getClientId();
                password = Objects.nonNull(x111.getPassword()) ? new String(x111.getPassword(), StandardCharsets.UTF_8)
                                                               : null;
                deviceEntity = findDeviceByToken(Objects.requireNonNull(deviceToken));
                deviceEntry = auth(deviceEntity, password);
                X112_QttConnack x112 = new X112_QttConnack();
                switch (deviceEntry.getOperation())
                {
                    case OP_NULL:
                        x112.rejectIdentifier();
                        break;
                    case OP_INVALID:
                        x112.rejectBadUserOrPassword();
                        break;
                    case OP_APPEND:
                        x112.responseOk();
                        break;
                }
                return new Pair<>(deviceEntry, x112);
        }
        return null;
    }

    @Override
    public IPair receive(IProtocol input)
    {
        switch (input.serial())
        {
            case X113_QttPublish.COMMAND:
                X113_QttPublish x113 = (X113_QttPublish) input;
                long origin = x113.getSession()
                                  .getIndex();
                MessageEntity msg = new MessageEntity();
                msg.setRetain(x113.isRetain());
                msg.setCmd(X113_QttPublish.COMMAND);
                msg.setDirection(Direction.CLIENT_TO_SERVER.getShort());
                msg.setMsgId(x113.getMsgId());
                msg.setOrigin(origin);
                msg.setOwner(x113.getLevel()
                                 .getValue() < IQoS.Level.EXACTLY_ONCE.getValue() ? OWNER_SERVER
                                                                                  : OWNER_CLIENT);
                MessageBody messageBody = new MessageBody();
                messageBody.setTopic(x113.getTopic());
                messageBody.setPayload(x113.getPayload());
                msg.setPayload(messageBody);
                msg = _MessageRepository.save(msg);
                _Logger.info("receive save: %s", msg);
                break;
            case X114_QttPuback.COMMAND:
                break;
            case X116_QttPubrel.COMMAND:
                X116_QttPubrel x116 = (X116_QttPubrel) input;
                origin = x116.getSession()
                             .getIndex();
                List<MessageEntity> msgList = _MessageRepository.findAllByOriginAndMsgIdAndDirectionAndOwner(origin,
                                                                                                             x116.getMsgId(),
                                                                                                             Direction.CLIENT_TO_SERVER.getShort(),
                                                                                                             OWNER_CLIENT);
                if (msgList != null) {
                    msgList.forEach(messageEntity -> messageEntity.setOwner(OWNER_SERVER));
                    if (!msgList.isEmpty()) {
                        return new Pair<>(_MessageRepository.saveAll(msgList)
                                                            .stream()
                                                            .map(entity ->
                                                            {
                                                                MessageBody entityPayload = entity.getPayload();
                                                                if (entityPayload == null) { return null; }
                                                                MessageEntry messageEntry = new MessageEntry();
                                                                messageEntry.setPrimaryKey(entity.getId());
                                                                messageEntry.setDirection(entity.getDirection());
                                                                messageEntry.setTopic(entityPayload.getTopic());
                                                                messageEntry.setPayload(entityPayload.getPayload());
                                                                messageEntry.setOrigin(entity.getOrigin());
                                                                messageEntry.setDestination(entity.getTarget());
                                                                return messageEntry;
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList()),
                                          MessageEntry.class);
                    }
                }
                _Logger.info("receive X116_QttPubrel: %s", x116);
                return new Pair<>(msgList, null);
        }
        return null;
    }

    public void send(IProtocol output)
    {
        switch (output.serial())
        {
            case X113_QttPublish.COMMAND:
                X113_QttPublish x113 = (X113_QttPublish) output;
                long target = x113.getSession()
                                  .getIndex();
                MessageEntity msg = new MessageEntity();
                msg.setRetain(x113.isRetain());
                msg.setCmd(X113_QttPublish.COMMAND);
                msg.setDirection(Direction.SERVER_TO_CLIENT.getShort());
                msg.setMsgId(x113.getMsgId());
                msg.setTarget(target);
                msg.setOwner(x113.getLevel()
                                 .getValue() > IQoS.Level.ALMOST_ONCE.getValue() ? OWNER_SERVER
                                                                                 : OWNER_CLIENT);
                MessageBody messageBody = new MessageBody();
                messageBody.setTopic(x113.getTopic());
                messageBody.setPayload(x113.getPayload());
                msg.setPayload(messageBody);
                msg = _MessageRepository.save(msg);
                _Logger.info("send save: %s", msg);
                break;
            case X116_QttPubrel.COMMAND:
                X116_QttPubrel x116 = (X116_QttPubrel) output;
                target = x116.getSession()
                             .getIndex();
                List<MessageEntity> msgList = _MessageRepository.findAllByTargetAndMsgIdAndDirectionAndOwner(target,
                                                                                                             x116.getMsgId(),
                                                                                                             Direction.SERVER_TO_CLIENT.getShort(),
                                                                                                             OWNER_SERVER);
                msgList.forEach(messageEntity -> messageEntity.setOwner(OWNER_CLIENT));
                _MessageRepository.saveAll(msgList);
                break;
        }
    }

    @SafeVarargs
    public final void localBizSend(long deviceId, IControl<ZContext>... toSends)
    {
        _DeviceNode.localBizSend(deviceId, toSends);
    }

    public void localBizClose(long deviceId)
    {
        _DeviceNode.localBizClose(deviceId);
    }

    public MessageBody getMessageById(long unique)
    {
        return _MessageRepository.findById(unique)
                                 .map(MessageEntity::getPayload)
                                 .orElse(null);
    }

    public List<MessageBody> listMessageByOriginAndMsgIdAfter(long target, long offset)
    {
        return _MessageRepository.findAllByOriginAndMsgIdAfter(target, offset)
                                 .stream()
                                 .map(MessageEntity::getPayload)
                                 .collect(Collectors.toList());
    }

    public List<MessageBody> listByTopic(String topic, int limit)
    {
        return _MessageRepository.listByTopic(topic, limit)
                                 .stream()
                                 .map(MessageEntity::getPayload)
                                 .collect(Collectors.toList());
    }
}
