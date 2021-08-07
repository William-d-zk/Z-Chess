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

package com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.service;

import com.isahl.chess.bishop.io.mqtt.command.X113_QttPublish;
import com.isahl.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.isahl.chess.bishop.io.mqtt.model.DeviceSubscribe;
import com.isahl.chess.bishop.io.mqtt.service.IQttRouter;
import com.isahl.chess.bishop.io.mqtt.service.IQttStorage;
import com.isahl.chess.king.base.inf.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.model.BrokerMsgEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.model.SessionEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.repository.IMessageJpaRepository;
import com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.repository.ISessionRepository;
import com.isahl.chess.queen.io.core.inf.IQoS;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.cache.CacheManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class QttStorageService
        implements IQttStorage,
                   IValid
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getName());

    private final IMessageJpaRepository              _MessageRepository;
    private final ISessionRepository                 _SessionRepository;
    private final CacheManager                       _CacheManager;
    private final Random                             _Random;
    private final TimeWheel                          _TimeWheel;
    private final ScheduleHandler<BrokerMsgEntity>   _StorageCleaner;
    private final ScheduleHandler<QttStorageService> _StorageHourCleaner;

    @Autowired
    public QttStorageService(IMessageJpaRepository messageRepository,
                             ISessionRepository sessionRepository,
                             CacheManager cacheManager,
                             TimeWheel timeWheel)
    {
        _MessageRepository = messageRepository;
        _SessionRepository = sessionRepository;
        _CacheManager = cacheManager;
        _TimeWheel = timeWheel;
        _Random = new Random(System.nanoTime());
        _StorageCleaner = new ScheduleHandler<>(Duration.ofMinutes(5), this::cleanup);
        _StorageHourCleaner = new ScheduleHandler<>(Duration.ofHours(1), true, QttStorageService::cleanup);
    }

    @PostConstruct
    void initCache() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "message_cache_msg_id",
                                  Long.class,
                                  Integer.class,
                                  Duration.of(2, MINUTES));
        _TimeWheel.acquire(this, _StorageHourCleaner);
    }

    @Override
    public boolean brokerStorage(int msgId, String topic, byte[] content, long target)
    {
        BrokerMsgEntity message = new BrokerMsgEntity();
        message.setMsgId(msgId);
        message.setTarget(target);
        message.setTopic(topic);
        message.setContent(content);
        message.setId(String.format(BrokerMsgEntity.BROKER_PRIMARY_FORMAT, target, msgId));
        try {
            _MessageRepository.save(message);
            _TimeWheel.acquire(message, _StorageCleaner);
        }
        catch(Throwable e) {
            _Logger.warning("local storage msg %s failed", message.getId(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteMessage(int msgId, long target)
    {
        String primaryKey = String.format(BrokerMsgEntity.BROKER_PRIMARY_FORMAT, target, msgId);
        try {
            _MessageRepository.deleteById(primaryKey);
        }
        catch(Throwable e) {
            _Logger.warning("local delete msg %s failed", primaryKey, e);
            return false;
        }
        return true;
    }

    @Override
    public int generateMsgId(long target)
    {
        return getNew(target);
    }

    @Override
    public void receivedStorage(int msgId, String topic, byte[] content, long origin)
    {
        BrokerMsgEntity message = new BrokerMsgEntity();
        message.setMsgId(msgId);
        message.setOrigin(origin);
        message.setTopic(topic);
        message.setContent(content);
        message.setId(String.format(BrokerMsgEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
        try {
            _MessageRepository.save(message);
        }
        catch(Throwable e) {
            _Logger.warning("local storage msg %s failed", message.getId(), e);
        }
    }

    @Override
    public boolean hasReceived(int msgId, long origin)
    {
        return _MessageRepository.existsById(String.format(BrokerMsgEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
    }

    @Override
    public X113_QttPublish takeStorage(int msgId, long origin)
    {
        String primaryKey = String.format(BrokerMsgEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId);
        try {
            Optional<BrokerMsgEntity> receivedOptional = _MessageRepository.findById(primaryKey);
            if(receivedOptional.isPresent()) {
                BrokerMsgEntity received = receivedOptional.get();
                X113_QttPublish x113 = new X113_QttPublish();
                x113.setTopic(received.getTopic());
                x113.putPayload(received.getContent());
                _MessageRepository.deleteById(primaryKey);
                return x113;
            }
        }
        catch(Throwable e) {
            _Logger.warning("miss received %s", primaryKey, e);
        }
        return null;
    }

    @Override
    public void sessionOnLogin(long session, IQttRouter router, X111_QttConnect x111)
    {
        Optional<SessionEntity> sessionEntityOptional = _SessionRepository.findById(session);
        if(sessionEntityOptional.isPresent()) {
            SessionEntity sessionEntity = sessionEntityOptional.get();
            sessionEntity.afterQuery();
            DeviceSubscribe deviceSubscribe = sessionEntity.getDeviceSubscribe();
            if(deviceSubscribe != null && !deviceSubscribe.getSubscribes()
                                                          .isEmpty())
            {
                deviceSubscribe.getSubscribes()
                               .forEach((topic, level)->router.subscribe(topic, level, session));
            }
            if(sessionEntity.getWillTopic() != null) {
                IQttRouter.Subscribe subscribe = router.subscribe(sessionEntity.getWillTopic(),
                                                                  sessionEntity.getWillLevel(),
                                                                  0);
                if(sessionEntity.isWillRetain()) {
                    X113_QttPublish retained = new X113_QttPublish();
                    retained.setTopic(sessionEntity.getWillTopic());
                    retained.setLevel(sessionEntity.getWillLevel());
                    retained.putPayload(sessionEntity.getWillPayload());
                    subscribe.mRetained = retained;
                }
            }
        }
        else {
            SessionEntity sessionEntity = new SessionEntity();
            sessionEntity.setId(session);
            sessionEntity.setWillRetain(x111.isWillRetain());
            sessionEntity.setWillTopic(x111.getWillTopic());
            sessionEntity.setWillLevel(x111.getWillLevel());
            sessionEntity.setWillPayload(x111.getWillMessage());
            sessionEntity.beforeSave();
            _SessionRepository.save(sessionEntity);
        }
    }

    @Override
    public void sessionOnSubscribe(long session, String topic, IQoS.Level level)
    {
        Optional<SessionEntity> sessionEntityOptional = _SessionRepository.findById(session);
        if(sessionEntityOptional.isPresent()) {
            SessionEntity sessionEntity = sessionEntityOptional.get();
            sessionEntity.afterQuery();
            sessionEntity.getDeviceSubscribe()
                         .subscribe(topic, level);
            sessionEntity.beforeSave();
            _SessionRepository.save(sessionEntity);
        }
    }

    @Override
    public void sessionOnUnsubscribe(long session, String topic)
    {
        Optional<SessionEntity> sessionEntityOptional = _SessionRepository.findById(session);
        if(sessionEntityOptional.isPresent()) {
            SessionEntity sessionEntity = sessionEntityOptional.get();
            sessionEntity.afterQuery();
            sessionEntity.getDeviceSubscribe()
                         .unsubscribe(topic);
            sessionEntity.beforeSave();
            _SessionRepository.save(sessionEntity);
        }
    }

    @Override
    public void cleanSession(long session)
    {
        if(_SessionRepository.existsById(session)) {_SessionRepository.deleteById(session);}
    }

    @Cacheable(key = "#key",
               value = "message_cache_msg_id")
    public int getLast(long key)
    {
        return _Random.nextInt() & 0xFFFF;
    }

    @CachePut(key = "#key",
              value = "message_cache_msg_id")
    public int getNew(long key)
    {
        return (getLast(key) + 1) & 0xFFFF;
    }

    private void cleanup(BrokerMsgEntity msgEntity)
    {
        Optional<BrokerMsgEntity> inDbOptional = _MessageRepository.findById(msgEntity.getId());
        if(inDbOptional.isPresent()) {
            BrokerMsgEntity inDb = inDbOptional.get();
            _MessageRepository.delete(inDb);
        }
    }

    private static void cleanup(QttStorageService self)
    {
        LocalDateTime idleTime = LocalDateTime.now()
                                              .minusHours(1);
        try {
            List<BrokerMsgEntity> idleHours = self._MessageRepository.findAll((root, criteriaQuery, criteriaBuilder)->{
                return criteriaQuery.where(criteriaBuilder.lessThan(root.get("createdAt"), idleTime))
                                    .getRestriction();
            });
            self._MessageRepository.deleteAll(idleHours);
        }
        catch(Throwable e) {
            self._Logger.warning("cycle cleaner x failed", e);
        }
    }
}
