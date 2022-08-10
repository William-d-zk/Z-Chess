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
package com.isahl.chess.queen.io.core.net.socket;

import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IExchanger;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.slf4j.event.Level;

import java.util.*;

/**
 * 所有io 管理器的父类，存在一定的存储空间的浪费。
 * 在简单场景中 client端存在大量的存储空间浪费。
 * 单一的多对多client 是不存在local cluster server 这三个子域的
 * 不过可以通过覆盖ISocketConfig 的方案削减空间占用。
 *
 * @author William.d.zk
 */
public abstract class AioManager
        implements IExchanger
{
    protected final Logger _Logger = Logger.getLogger("io.queen." + getClass().getSimpleName());

    private final Map<Long, ISession>[]              _Index2SessionMaps;
    private final Map<Long, Set<ISession>>[]         _Prefix2SessionMaps;
    private final Set<ISession>[]                    _SessionsSets;
    private final IAioConfig                         _AioConfig;
    private final Map<Long, Long>                    _Index2RouteMap;
    private final Map<Integer, IoFactory<IProtocol>> _FactoryMap;

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
        _Index2RouteMap = new HashMap<>(23);
        _FactoryMap = new HashMap<>();
        Arrays.setAll(_SessionsSets, slot->_AioConfig.isDomainActive(slot) ? new HashSet<>(1 << getConfigPower(slot)) : null);
        Arrays.setAll(_Index2SessionMaps, slot->_AioConfig.isDomainActive(slot) ? new HashMap<>(1 << getConfigPower(slot)) : null);
        Arrays.setAll(_Prefix2SessionMaps, slot->_AioConfig.isDomainActive(slot) ? new HashMap<>(23) : null);
    }

    @Override
    public void route(long index, long prefix)
    {
        Map<Long, Set<ISession>> prefix2SessionMap = _Prefix2SessionMaps[getSlot(prefix)];
        Set<ISession> sessions = prefix2SessionMap.get(prefix);
        if(sessions != null) {
            _Index2RouteMap.putIfAbsent(index, prefix);
        }
    }

    @Override
    public void dropPrefix(long prefix)
    {
        Map<Long, Set<ISession>> prefix2SessionMap = _Prefix2SessionMaps[getSlot(prefix)];
        Optional.of(prefix2SessionMap.remove(prefix))
                .ifPresent(Set::clear);
        _Index2RouteMap.entrySet()
                       .removeIf(entry->entry.getValue() == prefix);
    }

    protected int getConfigPower(int slot)
    {
        if(ZUID.MAX_TYPE < slot || slot < 0) {throw new IllegalArgumentException("slot: " + slot);}
        return _AioConfig.getSizePower(slot);
    }

    protected static int getSlot(long index)
    {
        return (int) ((index & ZUID.TYPE_MASK) >>> ZUID.TYPE_SHIFT);
    }

    @Override
    public void addSession(ISession session)
    {
        int slot = getSlot(session.index());
        _SessionsSets[slot].add(session);
        if(!_Logger.isEnable(Level.DEBUG)) {return;}
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
        if(session != null) {
            int slot = getSlot(session.index());
            _SessionsSets[slot].remove(session);
            long index = session.index();
            cleanIndex(index);
            //multi-bind 清理的时候 无需重复清理 prefix 映射
            if(session.isMultiBind() && session.bindIndex() != null) {
                for(long i : session.bindIndex()) {
                    if(_Index2SessionMaps[slot].remove(i, session)) {
                        _Index2RouteMap.remove(i);
                    }
                }
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
         * 2:相同 _Index 在不同的 Session 上登录，产生覆盖 Session 的情况。
         */
        long idx = session.index();
        if((idx & INVALID_INDEX) != NULL_INDEX && idx != _NewIdx && !session.isMultiBind()) {
            // session 已经 mapping 过了 且 不允许多绑定结构
            _Index2SessionMaps[getSlot(idx)].remove(idx);
        }
        // 检查可能覆盖的 Session 是否存在,_Index 已登录过
        ISession old = _Index2SessionMaps[getSlot(_NewIdx)].put(_NewIdx, session);
        if((idx & INVALID_INDEX) == NULL_INDEX || !session.isMultiBind()) {
            //首次登陆 或 执行唯一登陆逻辑[覆盖]
            session.index(_NewIdx);
        }
        else {
            //session 持有multi-bind 特征，且已经index绑定过了
            session.bindIndex(_NewIdx);
        }
        if(old != null && old != session) {
            // 发生 index → session 的映射变更
            if(old.index() == _NewIdx) {
                //发生了 session.index  映射的变更,登录发生在了新的session上
                return old;
            }
            else if(old.isMultiBind()) {
                //old-session 是允许多绑定场景时，发生的是multi-bind自身的迁移情况
                old.unbindIndex(_NewIdx);
            }
            else {
                _Logger.fetal("被覆盖的session 持有不同的index，%#x <=> old: %#x", _NewIdx, old.index());
                ISession old1 = _Index2SessionMaps[getSlot(old.index())].get(old.index());
                /*
                 * oldIndex bind old 已在 Map 完成其他的新的绑定关系。
                 * 由于MapSession是线程安全的，并不应该出现此种情况
                 */
                if(old1 == old) {
                    _Logger.fetal("old1 == old → Ignore, 检查MapSession 是否存在线程安全问题");// Ignore
                }
                else if(old1 == null) {
                    _Logger.debug("old1 == null → old.index invalid");// old.index 已失效
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
                if(getSlot(prefix) == slot) {
                    prefix2SessionMap.computeIfAbsent(prefix, k->new TreeSet<>())
                                     .add(session);
                    session.bindPrefix(prefix);
                }
            }
        }
        return oldSession;
    }

    @Override
    public void cleanSessionWithPrefix(ISession session, long prefix)
    {
        Map<Long, Set<ISession>> prefix2SessionMap = _Prefix2SessionMaps[getSlot(prefix)];
        // prefix2SessionMap 一定非NULL
        Optional.of(prefix2SessionMap.get(prefix))
                .ifPresent(set->set.remove(session));
    }

    @Override
    public ISession cleanIndex(long index)
    {
        ISession session = _Index2SessionMaps[getSlot(index)].remove(index);
        if(session != null) { // local manage
            long[] bindPrefix = session.getPrefixArray();
            if(bindPrefix != null) {
                for(long prefix : bindPrefix) {
                    cleanSessionWithPrefix(session, prefix);
                }
            }
        }
        else {
            //remote peer manage
            _Index2RouteMap.remove(index);
        }
        return session;
    }

    @Override
    public ISession findSessionByIndex(long index)
    {
        return _Index2SessionMaps[getSlot(index)].get(index);
    }

    @Override
    public ISession findSessionOverIndex(long index)
    {
        Long prefix = _Index2RouteMap.get(index);
        return prefix != null ? fairLoadSessionByPrefix(prefix) : null;
    }

    @Override
    public int getSessionCountByPrefix(long prefix)
    {
        return _Prefix2SessionMaps[getSlot(prefix)].size();
    }

    @Override
    public ISession fairLoadSessionByPrefix(long prefix)
    {
        Set<ISession> sessions = _Prefix2SessionMaps[getSlot(prefix)].get(prefix);
        if(sessions != null) {
            if(sessions.size() > 1) {
                ISession session = sessions.stream()
                                           .min(Comparator.comparing(s->s.prefixLoad(prefix)))
                                           .get();
                session.prefixHit(prefix);
                return session;
            }
            else if(sessions.size() == 1) {
                return sessions.iterator()
                               .next();
            }
        }
        return null;
    }

    public Set<ISession> getSessionSetWithType(int typeSlot)
    {
        return typeSlot > ZUID.MAX_TYPE ? null : _SessionsSets[typeSlot];
    }

    public Collection<ISession> getMappedSessionsWithType(int typeSlot)
    {
        return typeSlot > ZUID.MAX_TYPE ? null : _Index2SessionMaps[typeSlot].values();
    }

    public void registerFactory(IoFactory<IProtocol> factory)
    {
        _FactoryMap.putIfAbsent(factory.serial(), factory);
    }

    @Override
    public IoFactory<IProtocol> findIoFactoryBySerial(int factorySerial)
    {
        return _FactoryMap.get(factorySerial);
    }
}
