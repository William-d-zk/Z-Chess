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
package com.isahl.chess.queen.io.core.async;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;
import org.slf4j.event.Level;

import java.util.*;

import static com.isahl.chess.queen.io.core.inf.ISession.PREFIX_MAX;

/**
 * 所有io 管理器的父类，存在一定的存储空间的浪费。
 * 在简单场景中 client端存在大量的存储空间浪费。
 * 单一的多对多client 是不存在local cluster server 这三个子域的
 * 不过可以通过覆盖ISocketConfig 的方案削减空间占用。
 *
 * @author William.d.zk
 */
public class AioManager
        implements ISessionManager
{
    protected final Logger                     _Logger = Logger.getLogger(
            "io.queen.session." + getClass().getSimpleName());
    private final   Map<Long, ISession>[]      _Index2SessionMaps;
    private final   Map<Long, Set<ISession>>[] _Prefix2SessionMaps;
    private final   Set<ISession>[]            _SessionsSets;
    private final   IAioConfig                 _AioConfig;

    public ISocketConfig getSocketConfig(int type)
    {
        return _AioConfig.getSocketConfig(type);
    }

    @SuppressWarnings("unchecked")
    public AioManager(IAioConfig config)
    {
        final int _TYPE_COUNT = ZUID.MAX_TYPE + 1;
        _AioConfig = config;
        _Index2SessionMaps = new Map[_TYPE_COUNT];
        _Prefix2SessionMaps = new Map[_TYPE_COUNT];
        _SessionsSets = new Set[_TYPE_COUNT];
        Arrays.setAll(_SessionsSets,
                      slot->_AioConfig.isDomainActive(slot) ? new HashSet<>(1 << getConfigPower(slot)) : null);
        Arrays.setAll(_Index2SessionMaps,
                      slot->_AioConfig.isDomainActive(slot) ? new HashMap<>(1 << getConfigPower(slot)) : null);
        Arrays.setAll(_Prefix2SessionMaps, slot->_AioConfig.isDomainActive(slot) ? new HashMap<>(23) : null);
    }

    protected int getConfigPower(int slot)
    {
        if(ZUID.MAX_TYPE < slot || slot < 0) { throw new IllegalArgumentException("slot: " + slot); }
        return _AioConfig.getSizePower(slot);
    }

    protected static int getSlot(long index)
    {
        return (int) ((index & ZUID.TYPE_MASK) >>> ZUID.TYPE_SHIFT);
    }

    @Override
    public void addSession(ISession session)
    {
        int slot = getSlot(session.getIndex());
        _SessionsSets[slot].add(session);
        if(!_Logger.isEnable(Level.DEBUG)) { return; }
        _Logger.debug(String.format("%s add session -> set slot:%s", getClass().getSimpleName(), switch(slot) {
            case ZUID.TYPE_CONSUMER_SLOT -> "CONSUMER";
            case ZUID.TYPE_INTERNAL_SLOT -> "INTERNAL";
            case ZUID.TYPE_PROVIDER_SLOT -> "PROVIDER";
            case ZUID.TYPE_CLUSTER_SLOT -> "CLUSTER";
            default -> "Illegal";
        }));
    }

    @Override
    public void rmSession(ISession session)
    {
        int slot = getSlot(session.getIndex());
        _SessionsSets[slot].remove(session);
        long[] prefixArray = session.getPrefixArray();
        if(prefixArray != null) {
            for(long prefix : prefixArray) {
                _Prefix2SessionMaps[slot].get(prefix & PREFIX_MAX)
                                         .remove(session);
            }
        }
        _Index2SessionMaps[slot].remove(session.getIndex(), session);
        if(session.isMultiBind() && session.getBindIndex() != null) {
            for(long i : session.getBindIndex()) {
                _Index2SessionMaps[slot].remove(i, session);
            }
        }
    }

    /**
     * @return 正常情况下返回 _Index 返回 NULL_INDEX 说明 Map 失败。 或返回被覆盖的 OLD-INDEX 需要对其进行
     * PortChannel 的清理操作。
     */
    private ISession mapSession(final long _NewIdx, ISession session)
    {
        _Logger.debug("session manager map-> %#x,%s", _NewIdx, session);
        if(_NewIdx == INVALID_INDEX || (_NewIdx & INVALID_INDEX) == NULL_INDEX) {
            throw new IllegalArgumentException("invalid index");
        }
        /*
         * 1:相同 Session 不同 _Index 进行登录，产生多个 _Index 对应 相同 Session 的情况
         * 2:相同 _Index 在不同的 Session 上登录，产生覆盖
         * Session 的情况。
         */
        long sessionIdx = session.getIndex();
        if((sessionIdx & INVALID_INDEX) != NULL_INDEX && sessionIdx != _NewIdx && !session.isMultiBind()) {
            // session 已经 mapping 过了 且 不允许多绑定结构
            _Index2SessionMaps[getSlot(sessionIdx)].remove(sessionIdx);
        }
        // 检查可能覆盖的 Session 是否存在,_Index 已登录过
        ISession oldSession = _Index2SessionMaps[getSlot(_NewIdx)].put(_NewIdx, session);
        if((sessionIdx & INVALID_INDEX) == NULL_INDEX || !session.isMultiBind()) {
            //首次登陆 或 执行唯一登陆逻辑[覆盖]
            session.setIndex(_NewIdx);
        }
        else {
            //session 持有multi-bind 特征，且已经index绑定过了
            session.bindIndex(_NewIdx);
        }
        if(oldSession != null) {
            // 已经发生覆盖
            long oldIndex = oldSession.getIndex();
            if(oldIndex == _NewIdx) {
                if(oldSession != session) {
                    // 相同 _Index 登录在不同 Session 上登录
                    /*
                        被覆盖的 session 在 read EOF/TimeOut 时启动 Close
                        old-session.setIndex(仅包含Type信息）回到初始态
                        此操作在multi-bind情况下依然适用，index-mapping 将出现迁移
                        但持有type信息的情况下，multi-bind.index依然可以有效映射
                     */
                    oldSession.setIndex(oldIndex & ZUID.TYPE_MASK);
                    if(oldSession.isValid()) {
                        _Logger.warning("覆盖在线session: %s", oldSession);
                        return oldSession;
                    }
                }
                // 相同 session 上相同 _Index 重复登录
            }
            else {
                // 被覆盖的 session 持有不同的 _Index
                if(oldSession.isMultiBind()) {
                    //old-session 是允许多绑定场景时，发生的是multi-bind自身的迁移情况
                    oldSession.unbindIndex(_NewIdx);
                }
                else {
                    _Logger.fetal("被覆盖的session 持有不同的index，检查session.setIndex的引用;index: %d <=> old: %d",
                                  _NewIdx,
                                  oldIndex);
                    ISession oldMappedSession = _Index2SessionMaps[getSlot(oldIndex)].get(oldIndex);
                    /*
                     * oldIndex bind oldSession 已在 Map 完成其他的新的绑定关系。
                     * 由于MapSession是线程安全的，并不应该出现此种情况
                     */
                    if(oldMappedSession == oldSession) {
                        _Logger.fetal("oldMappedSession == oldSession -> Ignore, 检查MapSession 是否存在线程安全问题");// Ignore
                    }
                    else if(oldMappedSession == null) {
                        _Logger.debug("oldMappedSession == null -> oldIndex invalid");// oldIndex 已失效
                    }

                }
            }
        }
        return null;
    }

    @Override
    public ISession mapSession(final long _NewIdx, ISession session, long... prefixArray)
    {
        ISession oldSession = mapSession(_NewIdx, session);
        if(prefixArray != null) {
            int slot = getSlot(_NewIdx);
            Map<Long, Set<ISession>> prefix2SessionMap = _Prefix2SessionMaps[slot];
            for(long prefix : prefixArray) {
                if(getSlot(prefix) != slot) {
                    throw new IllegalArgumentException(String.format("index: %#x, prefix: %#x | slot error",
                                                                     _NewIdx,
                                                                     prefix));
                }
                prefix2SessionMap.computeIfAbsent(prefix, k->new TreeSet<>())
                                 .add(session);
                session.bindPrefix(prefix);
            }
        }
        return oldSession;
    }

    @Override
    public Collection<ISession> clearAllSessionByPrefix(long prefix)
    {
        Map<Long, Set<ISession>> prefix2SessionMap = _Prefix2SessionMaps[getSlot(prefix)];
        Set<ISession> sessions = prefix2SessionMap.get(prefix);
        sessions.forEach(this::clearSession);
        return sessions;

    }

    @Override
    public ISession clearSession(long index)
    {
        return _Index2SessionMaps[getSlot(index)].remove(index);
    }

    @Override
    public ISession findSessionByIndex(long index)
    {
        return _Index2SessionMaps[getSlot(index)].get(index);
    }

    @Override
    public int getSessionCountByPrefix(long prefix)
    {
        return _Prefix2SessionMaps[getSlot(prefix)].size();
    }

    @Override
    public ISession findSessionByPrefix(long prefix)
    {
        Set<ISession> sessions = _Prefix2SessionMaps[getSlot(prefix)].get(prefix);
        if(sessions != null) {
            Optional<ISession> optional = sessions.stream()
                                                  .min(Comparator.comparing(session->session.prefixLoad(prefix)));
            if(optional.isPresent()) {
                ISession session = optional.get();
                session.prefixHit(prefix);
                return session;
            }
        }
        return null;
    }

    public Collection<ISession> findAllByPrefix(long prefix)
    {
        return _Prefix2SessionMaps[getSlot(prefix)].get(prefix);
    }

    public Set<ISession> getSessionSetWithType(int typeSlot)
    {
        return typeSlot > ZUID.MAX_TYPE ? null : _SessionsSets[typeSlot];
    }

    public Collection<ISession> getMappedSessionsWithType(int typeSlot)
    {
        return typeSlot > ZUID.MAX_TYPE ? null : _Index2SessionMaps[typeSlot].values();
    }
}
