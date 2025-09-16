/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.db.local.service;

import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.pawn.endpoint.device.db.local.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.model.SessionEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.repository.IMsgStateRepository;
import com.isahl.chess.pawn.endpoint.device.db.local.repository.ISessionRepository;
import com.isahl.chess.pawn.endpoint.device.model.DeviceClient;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.pawn.endpoint.device.resource.features.IStateService;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IRoutable;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;
import com.isahl.chess.queen.io.core.features.model.routes.IThread.Subscribe;
import com.isahl.chess.queen.io.core.features.model.routes.IThread.Topic;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javafx.collections.transformation.SortedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.cache.CacheManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.*;

/**
 * @author william.d.zk
 * @date 2022-01-14
 */
@Service
public class StateService
        implements IValid,
                   IStateService
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final IDeviceService                _DeviceService;
    private final ISessionRepository            _SessionRepository;
    private final IMsgStateRepository           _MsgStateRepository;
    private final CacheManager                  _CacheManager;
    private final TimeWheel                     _TimeWheel;
    private final ScheduleHandler<StateService> _StorageHourCleaner;
    private final ZUID                          _ZUID;
    private final Map<Pattern, Subscribe>       _Topic2Subscribe;
    private final Map<Long, DeviceClient>       _ClientPool;

    @Autowired
    public StateService(IDeviceService deviceService,
                        ISessionRepository sessionRepository,
                        IMsgStateRepository messageRepository,
                        CacheManager cacheManager,
                        TimeWheel timeWheel,
                        IRaftConfig raftConfig)
    {
        _ZUID = raftConfig.getZUID();
        _DeviceService = deviceService;
        _SessionRepository = sessionRepository;
        _MsgStateRepository = messageRepository;
        _CacheManager = cacheManager;
        _TimeWheel = timeWheel;
        _Topic2Subscribe = new TreeMap<>(Comparator.comparing(Pattern::pattern));
        _ClientPool = new ConcurrentSkipListMap<>();
        _StorageHourCleaner = new ScheduleHandler<>(Duration.ofHours(1), true, StateService::cleanup);
    }

    @PostConstruct
    void initCache() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        EhcacheConfig.createCache(_CacheManager,
                                  "message_cache_msg_id",
                                  Long.class,
                                  Integer.class,
                                  Duration.of(2, MINUTES));

        //        EhcacheConfig.createCache(_CacheManager,
        //                                  "raft_log_entry",
        //                                  Long.class,
        //                                  LogEntry.class,
        //                                  Duration.of(30, SECONDS));
        _TimeWheel.acquire(this, _StorageHourCleaner);
    }

    public long getLast16(long key)
    {
        return getLast(key) & 0xFFFF;
    }

    @Cacheable(key = "#p0",
               condition = "#p0 != 0",
               value = "message_cache_msg_id")
    public long getLast(long key)
    {
        return _ZUID.getId();
    }

    @CachePut(key = "#p0",
              value = "message_cache_msg_id")
    public long getNew(long key)
    {
        return (getLast(key) + 1) & Long.MAX_VALUE;
    }

    @PreDestroy
    private void preDestroy()
    {
        try {
            cleanup(this);
        }
        catch(Throwable e) {
            _Logger.warning(e);
        }
    }

    private static void cleanup(StateService self)
    {
        LocalDateTime idleTime = LocalDateTime.now()
                                              .minusHours(1);
        try {
            //@formatter:off
            List<MsgStateEntity> idleMsgHours =
                    self._MsgStateRepository
                        .findAll((root, criteriaQuery, criteriaBuilder)->
                                        criteriaQuery.where(criteriaBuilder.lessThan(root.get("createdAt"), idleTime))
                                                     .getRestriction()
                                );
            List<SessionEntity> idleSessionHours =
                    self._SessionRepository
                        .findAll((root, criteriaQuery, criteriaBuilder)->
                                        criteriaQuery.where(criteriaBuilder.lessThan(root.get("updatedAt"), idleTime))
                                                     .getRestriction()
                                );

            if(!idleMsgHours.isEmpty()) {self._MsgStateRepository.deleteAll(idleMsgHours);}
            if(!idleSessionHours.isEmpty()) {
                self._SessionRepository.deleteAll(
                        self._ClientPool.isEmpty()
                            ? idleSessionHours
                            : idleSessionHours.stream()
                                              .filter(entity->self._ClientPool.containsKey(entity.getId()))
                                              .toList()
                                                  );
            }
            //@formatter:on
        }
        catch(Throwable e) {
            self._Logger.warning("cycle cleaner x failed", e);
        }
    }

    @Override
    public boolean onLogin(long session, boolean clean, long duration)
    {
        boolean present = _ClientPool.containsKey(session);
        Optional<SessionEntity> sOptional = _SessionRepository.findById(session);
        DeviceClient client;
        if(sOptional.isEmpty()) {
            client = _ClientPool.computeIfAbsent(session, k->new DeviceClient(session));
        }
        else {
            client = sOptional.get()
                              .client();
            _ClientPool.put(session, client);
        }
        client.setKeepAlive(duration);
        long device = session & ~ZUID.TYPE_MASK;
        client.of(_DeviceService.getOneDevice(device));
        if(clean) {
            if(!_Topic2Subscribe.isEmpty()) {
                _Topic2Subscribe.values()
                                .forEach(map->map.onDismiss(session));
            }
            if(sOptional.isPresent()) {
                _SessionRepository.deleteById(session);
            }
        }
        else {
            SessionEntity sessionEntity = sOptional.orElseGet(SessionEntity::new);
            sessionEntity.setId(session);
            sessionEntity.update(client);
            try {
                _SessionRepository.save(sessionEntity);
            }
            catch(Throwable e) {
                _Logger.warning("session[%#x] storage → db error", e, session);
            }
        }
        return present;
    }

    @Override
    public IProtocol dismiss(long session)
    {
        if(!_Topic2Subscribe.isEmpty()) {
            _Topic2Subscribe.values()
                            .forEach(map->map.onDismiss(session));
        }
        _ClientPool.computeIfPresent(session, (k, v)->{
            if(v.isClean()) {
                v.identifierSendingMap()
                 .clear();
                v.identifierReceivedMap()
                 .clear();
                return null;
            }
            return v;
        });
        IProtocol protocol = null;
        Optional<SessionEntity> optional = _SessionRepository.findById(session);
        if(optional.isPresent()) {
            SessionEntity se = optional.get();
            DeviceClient client = se.client();
            if(client != null && client.getWillContent() != null) {
                IThread.Topic will = client.getWillContent();
                // TODO 从遗嘱存储中提取遗嘱信息。

            }
        }
        return null;
    }

    @Override
    public List<Long> listIndex()
    {
        return _ClientPool.keySet()
                          .stream()
                          .toList();
    }

    @Override
    public List<SessionEntity> listStorages(Pageable pageable)
    {
        return _SessionRepository.findAll(pageable)
                                 .toList();
    }

    @Override
    public DeviceClient getClient(long session)
    {
        return _ClientPool.get(session);
    }

    @Override
    public <P extends IRoutable & IProtocol & IQoS> void store(long origin, long msgId, P body)
    {
        DeviceClient client = getClient(origin);
        if(client == null || client.identifierReceivedMap()
                                   .compute(msgId, (k, old)->{
                                       if(old != null) {
                                           _Logger.debug("duplicate received: %s", body);
                                       }
                                       return body;
                                   }) != null)
        {
            _Logger.debug("store received message :%s", body);
            if(body.level()
                   .getValue() > IQoS.Level.ALMOST_ONCE.getValue())
            {
                MsgStateEntity message = new MsgStateEntity();
                message.setTopic(body.topic());
                message.setOrigin(origin);
                message.setMsgId(msgId);
                message.setTarget(_ZUID.getPeerId());
                message.setContent(body.payload());
                message.setId(String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
                try {
                    _MsgStateRepository.save(message);
                }
                catch(Throwable e) {
                    _Logger.warning("local storage received msg %s failed", message.getId(), e);
                }
            }
        }
    }

    @Override
    public MsgStateEntity extract(long origin, long msgId)
    {
        String primaryKey = String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId);
        try {

            Optional<MsgStateEntity> optional = _MsgStateRepository.findById(primaryKey);
            if(optional.isPresent()) {
                _MsgStateRepository.deleteById(primaryKey);
                return optional.get();
            }
        }
        catch(Throwable e) {
            _Logger.warning("local delete msg %s failed", primaryKey, e);
        }
        return null;
    }

    @Override
    public <P extends IRoutable & IProtocol & IQoS> boolean add(long msgId, P body)
    {
        long target = body.target();
        DeviceClient client = getClient(target);

        if(client == null || client.identifierSendingMap()
                                   .putIfAbsent(msgId, body) == body)
        {
            _Logger.debug("add: %s", body);
            if(body.level()
                   .getValue() > IQoS.Level.ALMOST_ONCE.getValue())
            {
                MsgStateEntity message = new MsgStateEntity();
                message.setTopic(body.topic());
                message.setOrigin(_ZUID.getPeerId());
                message.setMsgId(msgId);
                message.setTarget(body.target());
                message.setContent(body.payload());
                message.setId(String.format(MsgStateEntity.BROKER_PRIMARY_FORMAT, body.target(), msgId));
                try {
                    _MsgStateRepository.save(message);
                }
                catch(Throwable e) {
                    _Logger.warning("local add broker msg %s failed", message.getId(), e);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean drop(long target, long msgId)
    {
        DeviceClient client = getClient(target);
        if(client != null) {
            client.identifierSendingMap()
                  .remove(msgId);
        }
        String key = String.format(MsgStateEntity.BROKER_PRIMARY_FORMAT, target, msgId);
        try {
            _MsgStateRepository.deleteById(key);
        }
        catch(Throwable e) {
            //ignore no exists key
            return false;
        }
        return true;
    }

    @Override
    public Subscribe onSubscribe(Topic topic, long session)
    {
        Optional<SessionEntity> optional = _SessionRepository.findById(session);
        if(optional.isPresent()) {
            SessionEntity sessionEntity = optional.get();
            sessionEntity.client()
                         .getSubscribes()
                         .add(topic);
            _SessionRepository.save(sessionEntity);
        }
        Subscribe subscribe = _Topic2Subscribe.computeIfAbsent(topic.pattern(), Subscribe::new);
        if(session != 0) {
            //@formatter:off
            if(subscribe.computeIfPresent(session,
                                          (key, old)->old.getValue() > topic.level()
                                                                            .getValue()
                                                      ? old : topic.level())
               == null)
            //@formatter:on
            {
                subscribe.onSubscribe(session, topic.level());
            }
        }
        return subscribe;
    }

    @Override
    public void onUnsubscribe(Topic topic, long session)
    {
        Optional<SessionEntity> optional = _SessionRepository.findById(session);
        if(optional.isPresent()) {
            SessionEntity sessionEntity = optional.get();
            sessionEntity.client()
                         .getSubscribes()
                         .remove(topic);
            _SessionRepository.save(sessionEntity);
        }
        _Topic2Subscribe.computeIfPresent(topic.pattern(), (k, o)->{
            o.onDismiss(session);
            return o.isEmpty() ? null : o;
        });
    }

    @Override
    public boolean exists(long origin, long msgId)
    {
        DeviceClient client = getClient(origin);
        return client == null || client.identifierReceivedMap()
                                       .containsKey(msgId) ||
               _MsgStateRepository.existsById(String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
    }

    @Override
    public Map<Pattern, Subscribe> mappings()
    {
        return _Topic2Subscribe;
    }

    @Override
    public List<Pattern> filter(String filter)
    {
        return _Topic2Subscribe.keySet().stream().filter(p->p.asMatchPredicate().test(filter)).toList();
    }
}
