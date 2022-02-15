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
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.pawn.endpoint.device.api.features.IStateService;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.SessionEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.repository.IMsgStateRepository;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.repository.ISessionRepository;
import com.isahl.chess.pawn.endpoint.device.model.DeviceClient;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IRoutable;
import com.isahl.chess.queen.io.core.features.model.routes.IThread.Subscribe;
import com.isahl.chess.queen.io.core.features.model.routes.IThread.Topic;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.rook.storage.cache.config.EhcacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.CacheManager;
import java.time.Duration;
import java.time.Instant;
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

    private final ISessionRepository  _SessionRepository;
    private final IMsgStateRepository _MessageRepository;
    private final CacheManager        _CacheManager;
    private final TimeWheel                               _TimeWheel;
    private final ScheduleHandler<MsgStateEntity>         _StorageCleaner;
    private final ScheduleHandler<StateService>           _StorageHourCleaner;
    private final ZUID                                    _ZUID;
    private final Map<Pattern, Subscribe>                 _Topic2Subscribe;
    private final Map<Long, Map<Long, IProtocol>>         _SessionWithIdentifierMap;
    private final NavigableMap<Long, Pair<Long, Instant>> _SessionIdleMap;

    @Autowired
    public StateService(ISessionRepository sessionRepository,
                        IMsgStateRepository messageRepository,
                        CacheManager cacheManager,
                        TimeWheel timeWheel,
                        IRaftConfig raftConfig)
    {
        _SessionRepository = sessionRepository;
        _MessageRepository = messageRepository;
        _CacheManager = cacheManager;
        _TimeWheel = timeWheel;
        _ZUID = raftConfig.getZUID();
        _Topic2Subscribe = new TreeMap<>(Comparator.comparing(Pattern::pattern));
        _SessionWithIdentifierMap = new ConcurrentSkipListMap<>();
        _SessionIdleMap = new ConcurrentSkipListMap<>();
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
        for(Iterator<Map.Entry<Long, Pair<Long, Instant>>> it = _SessionIdleMap.entrySet()
                                                                               .iterator(); it.hasNext(); ) {
            Map.Entry<Long, Pair<Long, Instant>> entry = it.next();
            long session = entry.getKey();
            IPair pair = entry.getValue();
            long keepalive = pair.getFirst();
            Instant idle = pair.getSecond();
            if(Instant.now()
                      .isAfter(idle.plusMillis(keepalive)))
            {
                _SessionWithIdentifierMap.remove(session);
                it.remove();
            }
        }
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
                    self._MessageRepository
                        .findAll((root, criteriaQuery, criteriaBuilder)->
                                        criteriaQuery.where(criteriaBuilder.lessThan(root.get("createdAt"), idleTime))
                                                     .getRestriction()
                                );
            List<SessionEntity> idleSessionHours =
                    self._SessionRepository
                        .findAll((root, criteriaQuery, criteriaBuilder)->
                                         criteriaQuery.where(criteriaBuilder.lessThan(root.get("createdAt"), idleTime))
                                                      .getRestriction()
                                );

            if(!idleMsgHours.isEmpty()) {self._MessageRepository.deleteAll(idleMsgHours);}
            if(!idleSessionHours.isEmpty()) {
                self._SessionRepository.deleteAll(
                        self._SessionWithIdentifierMap.isEmpty()
                            ? idleSessionHours
                            : idleSessionHours.stream()
                                              .filter(entity->self._SessionWithIdentifierMap.containsKey(entity.getId()))
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
    public boolean onLogin(long session, boolean clean, long keepalive)
    {
        boolean present = _SessionWithIdentifierMap.containsKey(session) || _SessionRepository.existsById(session);
        if(clean && !_Topic2Subscribe.isEmpty()) {
            _Topic2Subscribe.values()
                            .forEach(map->map.onDismiss(session));
            try {
                _SessionRepository.deleteById(session);
            }
            catch(Throwable e) {
                _Logger.warning("session[%#x] clean → db error", e, session);
            }
        }
        _SessionWithIdentifierMap.computeIfAbsent(session, k->new HashMap<>());
        _SessionIdleMap.computeIfAbsent(session, k->Pair.of(keepalive, Instant.now()));
        if(!clean) {
            Optional<SessionEntity> sOptional = _SessionRepository.findById(session);
            SessionEntity sessionEntity;
            sessionEntity = sOptional.orElseGet(SessionEntity::new);
            sessionEntity.setId(session);
            DeviceClient client = new DeviceClient(session);
            client.setKeepAlive(keepalive);
            sessionEntity.update(client);
            try {
                _SessionRepository.save(sessionEntity);
            }
            catch(Throwable e) {
                _Logger.warning("session [%#x] login state save ", e, session);
            }
        }
        return present;
    }

    @Override
    public SessionEntity load(long session)
    {
        return _SessionRepository.findById(session)
                                 .orElse(null);
    }

    @Override
    public List<SessionEntity> loadAll()
    {
        return _SessionRepository.findAll();
    }

    @Override
    public void onDismiss(long session)
    {
        if(!_Topic2Subscribe.isEmpty()) {
            _Topic2Subscribe.values()
                            .forEach(map->map.onDismiss(session));
        }
        _SessionWithIdentifierMap.computeIfPresent(session, (k, v)->{
            v.clear();
            return null;
        });
        _SessionIdleMap.remove(session);
        Optional<SessionEntity> sOptional = _SessionRepository.findById(session);
        if(sOptional.isPresent()) {
            SessionEntity sessionEntity = sOptional.get();
            DeviceClient client = sessionEntity.client();
            if(client != null) {

            }
        }

    }

    @Override
    public <P extends IRoutable & IProtocol> void store(long origin, long msgId, P body)
    {
        if(_SessionWithIdentifierMap.computeIfPresent(body.target(), (key, _MsgIdMessageMap)->{
            IProtocol old = _MsgIdMessageMap.put(msgId, body);
            if(old != null) {
                _Logger.debug("duplicate received: %s", body);
            }
            return _MsgIdMessageMap;
        }) == null)
        {
            final Map<Long, IProtocol> _LocalIdMessageMap = new HashMap<>(16);
            _LocalIdMessageMap.put(msgId, body);
            _SessionWithIdentifierMap.put(body.target(), _LocalIdMessageMap);
            _Logger.debug("first received: %s", body);
            MsgStateEntity message = new MsgStateEntity();
            message.setTopic(body.getTopic());
            message.setOrigin(origin);
            message.setMsgId(msgId);
            message.setTarget(_ZUID.getPeerId());
            message.setContent(body.payload());
            message.setId(String.format(MsgStateEntity.RECEIVER_PRIMARY_FORMAT, origin, msgId));
            try {
                _MessageRepository.save(message);
                _TimeWheel.acquire(message, _StorageCleaner);
            }
            catch(Throwable e) {
                _Logger.warning("local storage received msg %s failed", message.getId(), e);
            }
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
    public <P extends IRoutable & IProtocol & IQoS> void add(long msgId, P body)
    {
        if(_SessionWithIdentifierMap.computeIfPresent(body.target(), (key, _MsgIdMessageMap)->{
            IProtocol old = _MsgIdMessageMap.put(msgId, body);
            if(old != null) {
                _Logger.debug("duplicate add: %s", body);
            }
            return _MsgIdMessageMap;
        }) == null)
        {
            //previous == null
            final Map<Long, IProtocol> _LocalIdMessageMap = new HashMap<>(16);
            _LocalIdMessageMap.put(msgId, body);
            _SessionWithIdentifierMap.put(body.target(), _LocalIdMessageMap);
            _Logger.debug("first add: %s", body);
            if(body.getLevel()
                   .getValue() > IQoS.Level.ALMOST_ONCE.getValue())
            {
                MsgStateEntity message = new MsgStateEntity();
                message.setTopic(body.getTopic());
                message.setOrigin(_ZUID.getPeerId());
                message.setMsgId(msgId);
                message.setTarget(body.target());
                message.setContent(body.payload());
                message.setId(String.format(MsgStateEntity.BROKER_PRIMARY_FORMAT, body.target(), msgId));
                try {
                    _MessageRepository.save(message);
                    _TimeWheel.acquire(message, _StorageCleaner);
                }
                catch(Throwable e) {
                    _Logger.warning("local add broker msg %s failed", message.getId(), e);
                }
            }
        }
    }

    @Override
    public boolean drop(long target, long msgId)
    {
        boolean[] ack = { false,
                          false,
                          false };
        ack[0] = _SessionWithIdentifierMap.computeIfPresent(target, (key, old)->{
            _Logger.debug("drop %d @ %#x", msgId, target);
            ack[1] = old.remove(msgId) != null;
            ack[2] = old.isEmpty();
            return old;
        }) != null;
        if(ack[0] && ack[2]) {
            _SessionIdleMap.computeIfPresent(target, (key, old)->Pair.of(old.getFirst(), Instant.now()));
        }
        String key = String.format(MsgStateEntity.BROKER_PRIMARY_FORMAT, target, msgId);
        boolean result = _MessageRepository.existsById(key) || (ack[0] && ack[1]);
        try {
            _MessageRepository.deleteById(key);
        }
        catch(Throwable e) {
            _Logger.warning("local delete msg %s failed", key, e);
        }
        return result;
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
