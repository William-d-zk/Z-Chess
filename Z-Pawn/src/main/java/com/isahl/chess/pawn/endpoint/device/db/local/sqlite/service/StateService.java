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

package com.isahl.chess.pawn.endpoint.device.db.local.sqlite.service;

import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.pawn.endpoint.device.api.features.IStateService;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.SessionEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.repository.IMessageRepository;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.repository.ISessionRepository;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IThread.Subscribe;
import com.isahl.chess.queen.io.core.features.model.routes.IThread.Topic;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.cache.CacheManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.MINUTES;

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

    private final ISessionRepository              _SessionRepository;
    private final IMessageRepository              _MessageRepository;
    private final CacheManager                    _CacheManager;
    private final TimeWheel                       _TimeWheel;
    private final ScheduleHandler<MsgStateEntity> _StorageCleaner;
    private final ScheduleHandler<StateService>   _StorageHourCleaner;
    private final ZUID                            _ZUID;
    private final Map<Pattern, Subscribe>         _Topic2Subscribe;
    private final Map<Long, Map<Long, IProtocol>> _SessionWithIdentifierMap;

    @Autowired
    public StateService(ISessionRepository sessionRepository,
                        IMessageRepository messageRepository,
                        CacheManager cacheManager,
                        TimeWheel timeWheel,
                        ZUID zuid)
    {
        _SessionRepository = sessionRepository;
        _MessageRepository = messageRepository;
        _CacheManager = cacheManager;
        _TimeWheel = timeWheel;
        _ZUID = zuid;
        _Topic2Subscribe = new TreeMap<>(Comparator.comparing(Pattern::pattern));
        _SessionWithIdentifierMap = new ConcurrentSkipListMap<>();
        _StorageCleaner = new ScheduleHandler<>(Duration.ofMinutes(5), this::cleanup);
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
        _TimeWheel.acquire(this, _StorageHourCleaner);
    }

    public long getLast16(long key)
    {
        return getLast(key) & 0xFFFF;
    }

    @Cacheable(key = "#key",
               value = "message_cache_msg_id")
    public long getLast(long key)
    {
        return _ZUID.getId();
    }

    @CachePut(key = "#key",
              value = "message_cache_msg_id")
    public long getNew(long key)
    {
        return (getLast(key) + 1) & Long.MAX_VALUE;
    }

    private void cleanup(MsgStateEntity msgEntity)
    {
        Optional<MsgStateEntity> inDbOptional = _MessageRepository.findById(msgEntity.getId());
        if(inDbOptional.isPresent()) {
            MsgStateEntity inDb = inDbOptional.get();
            _MessageRepository.delete(inDb);
        }
    }

    private static void cleanup(StateService self)
    {
        LocalDateTime idleTime = LocalDateTime.now()
                                              .minusHours(1);
        try {
            //@formatter:off
            List<MsgStateEntity> idleHours =
                    self._MessageRepository
                        .findAll((root, criteriaQuery, criteriaBuilder)->
                                        criteriaQuery.where(criteriaBuilder.lessThan(root.get("createdAt"), idleTime)).getRestriction()
                                );
            //@formatter:on
            if(!idleHours.isEmpty()) {self._MessageRepository.deleteAll(idleHours);}
        }
        catch(Throwable e) {
            self._Logger.warning("cycle cleaner x failed", e);
        }
    }

    @Override
    public boolean onLogin(long session, boolean clean)
    {
        Optional<SessionEntity> sOptional = _SessionRepository.findById(session);
        SessionEntity sessionEntity;
        sessionEntity = sOptional.orElseGet(SessionEntity::new);
        sessionEntity.setClean(clean);
        try {
            if(clean) {
                _Topic2Subscribe.values()
                                .forEach(map->map.onDismiss(session));
            }
            _SessionRepository.save(sessionEntity);
            return true;
        }
        catch(Throwable e) {
            _Logger.warning("session [%#x] login state save ", session);
        }
        return false;
    }

    @Override
    public SessionEntity load(long session)
    {
        return _SessionRepository.findById(session)
                                 .orElse(null);
    }

    @Override
    public void onDismiss(long session)
    {
        Optional<SessionEntity> sOptional = _SessionRepository.findById(session);
        if(sOptional.isPresent()) {
            SessionEntity present = sOptional.get();
            if(present.isClean()) {
                _Topic2Subscribe.values()
                                .forEach(map->map.onDismiss(session));
            }
            _SessionRepository.deleteById(session);
        }
        else {
            _Topic2Subscribe.values()
                            .forEach(map->map.onDismiss(session));
        }
    }

    @Override
    public void store(long origin, long msgId, String topic, byte[] payload)
    {
        MsgStateEntity message = new MsgStateEntity();
        message.setTopic(topic);
        message.setOrigin(origin);
        message.setMsgId(msgId);
        message.setTarget(_ZUID.getPeerId());
        message.setContent(payload);
        message.setId(String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
        try {
            _MessageRepository.save(message);
            _TimeWheel.acquire(message, _StorageCleaner);
        }
        catch(Throwable e) {
            _Logger.warning("local storage received msg %s failed", message.getId(), e);
        }
    }

    @Override
    public void extract(long origin, long msgId)
    {
        String primaryKey = String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId);
        try {

            Optional<MsgStateEntity> optional = _MessageRepository.findById(primaryKey);
            if(optional.isPresent()) {
                _MessageRepository.deleteById(primaryKey);
            }
        }
        catch(Throwable e) {
            _Logger.warning("local delete msg %s failed", primaryKey, e);
        }
    }

    @Override
    public MsgStateEntity query(long origin, long msgId)
    {
        return _MessageRepository.findById(String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId))
                                 .orElse(null);
    }

    @Override
    public void add(long target, long msgId, String topic, byte[] payload)
    {
        MsgStateEntity message = new MsgStateEntity();
        message.setTopic(topic);
        message.setOrigin(_ZUID.getPeerId());
        message.setMsgId(msgId);
        message.setTarget(target);
        message.setContent(payload);
        message.setId(String.format(MsgStateEntity.BROKER_PRIMARY_FORMAT, target, msgId));
        try {
            _MessageRepository.save(message);
            _TimeWheel.acquire(message, _StorageCleaner);
        }
        catch(Throwable e) {
            _Logger.warning("local add broker msg %s failed", message.getId(), e);
        }

    }

    @Override
    public void drop(long target, long msgId)
    {
        String primaryKey = String.format(MsgStateEntity.BROKER_PRIMARY_FORMAT, target, msgId);
        try {

            Optional<MsgStateEntity> optional = _MessageRepository.findById(primaryKey);
            if(optional.isPresent()) {
                _MessageRepository.deleteById(primaryKey);
            }
        }
        catch(Throwable e) {
            _Logger.warning("local delete msg %s failed", primaryKey, e);
        }
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
        Map<Long, IProtocol> identifier = _SessionWithIdentifierMap.get(origin);
        if(identifier != null) {
            return identifier.containsKey(msgId);
        }
        return _MessageRepository.existsById(String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
    }

    @Override
    public Map<Pattern, Subscribe> mappings()
    {
        return _Topic2Subscribe;
    }
}
