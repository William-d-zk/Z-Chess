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
package com.tgx.chess.queen.io.core.async;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.ISocketConfig;
import com.tgx.chess.queen.config.QueenCode;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * 所有io 管理器的父类，存在一定的存储空间的浪费。
 * 在简单场景中 client端存在大量的存储空间浪费。
 * 单一的多对多client 是不存在local cluster server 这三个子域的
 * 不过可以通过覆盖ISocketConfig 的方案削减空间占用。
 * 
 * @author William.d.zk
 */
public abstract class AioSessionManager<C extends IContext<C>>
        implements
        ISessionManager<C>
{
    protected final Logger                      _Logger             = Logger.getLogger(getClass().getSimpleName());
    private final Map<Long,
                      ISession<C>>[]            _Index2SessionMaps  = new Map[4];
    private final Map<Long,
                      Set<ISession<C>>>[]       _Prefix2SessionMaps = new Map[4];
    private final Set<ISession<C>>[]            _SessionsSets       = new Set[4];
    private final IBizIoConfig                  _BizIoConfig;

    public ISocketConfig getSocketConfig(int bizType)
    {
        return _BizIoConfig.getBizSocketConfig(bizType);
    }

    public AioSessionManager(IBizIoConfig config)
    {
        _BizIoConfig = config;
        Arrays.setAll(_SessionsSets, slot -> new HashSet<>(1 << getConfigPower(slot)));
        Arrays.setAll(_Index2SessionMaps, slot -> new HashMap<>(1 << getConfigPower(slot)));
        Arrays.setAll(_Prefix2SessionMaps, slot -> new HashMap<>(23));
    }

    protected int getConfigPower(int slot)
    {
        switch (slot)
        {
            case CLIENT_SLOT:
            case LOCAL_SLOT:
            case SERVER_SLOT:
            case CLUSTER_SLOT:
                return _BizIoConfig.getSizePower(slot);
            default:
                throw new IllegalArgumentException("slot: " + slot);
        }
    }

    protected static <C extends IContext<C>> int getSlot(ISession<C> session)
    {
        return getSlot(session.getIndex());
    }

    protected static int getSlot(long index)
    {
        return (int) ((index & QueenCode.XID_MK) >>> 62);
    }

    @Override
    public void addSession(ISession<C> session)
    {
        int slot = getSlot(session);
        _Logger.info(String.format("%s add session -> set slot:%s",
                                   getClass().getSimpleName(),
                                   slot == CLIENT_SLOT ? "CLIENT"
                                                       : slot == LOCAL_SLOT ? "INTERNAL"
                                                                            : slot == SERVER_SLOT ? "SERVER"
                                                                                                  : "CLUSTER"));
        _SessionsSets[slot].add(session);
    }

    @Override
    public void rmSession(ISession<C> session)
    {
        _SessionsSets[getSlot(session)].remove(session);
    }

    /**
     * @return 正常情况下返回 _Index 返回 NULL_INDEX 说明 Map 失败。 或返回被覆盖的 OLD-INDEX 需要对其进行
     *         PortChannel 的清理操作。
     */
    private ISession<C> mapSession(long _Index, ISession<C> session)
    {
        _Logger.info("session manager map->(%d,%s)", _Index, session);
        if (_Index == INVALID_INDEX || _Index == NULL_INDEX) {
            throw new IllegalArgumentException(String.format("index error %d", _Index));
        }
        /*
         * 1:相同 Session 不同 _Index 进行登录，产生多个 _Index 对应 相同 Session 的情况 2:相同 _Index 在不同的 Session 上登录，产生覆盖 Session 的情况。
         */
        long sOldIndex = session.getIndex();
        if (sOldIndex != INVALID_INDEX) {
            _Index2SessionMaps[getSlot(session)].remove(sOldIndex);
        }
        // 检查可能覆盖的 Session 是否存在,_Index 已登录过
        ISession<C> oldSession = _Index2SessionMaps[getSlot(session)].put(_Index, session);
        session.setIndex(_Index);
        if (oldSession != null) {
            // 已经发生覆盖
            long oldIndex = oldSession.getIndex();
            if (oldIndex == _Index) {
                if (oldSession != session) {
                    // 相同 _Index 登录在不同 Session 上登录
                    /*
                     * 被覆盖的 Session 在 read EOF/TimeOut 时启动 Close 防止 rmSession 方法删除 _Index 的当前映射
                     */
                    oldSession.setIndex(INVALID_INDEX);
                    if (oldSession.isValid()) {
                        _Logger.warning("覆盖在线session: %s", oldSession);
                        return oldSession;
                    }
                }
                // 相同 Session 上相同 _Index 重复登录
            }
            // 被覆盖的 Session 持有不同的 _Index
            else {
                _Logger.fetal("被覆盖的session 持有不同的index，检查session.setIndex的引用;index: %d <=> old: %d", _Index, oldIndex);
                ISession<C> oldMappedSession = _Index2SessionMaps[getSlot(session)].get(oldIndex);
                /*
                 * oldIndex bind oldSession 已在 Map 完成其他的新的绑定关系。
                 * 由于MapSession是现成安全的，并不会存在此种情况
                 */
                if (oldMappedSession == oldSession) {
                    _Logger.fetal("oldMappedSession == oldSession -> Ignore, 检查MapSession 是否存在线程安全问题");// Ignore
                }
                else if (oldMappedSession == null) {
                    _Logger.debug("oldMappedSession == null -> oldIndex invalid");// oldIndex 已失效
                }
                // else oldIndex 已完成其他的绑定过程无需做任何处理。
            }
        }
        return null;
    }

    @Override
    public ISession<C> mapSession(long index, ISession<C> session, long... prefixArray)
    {
        ISession<C> old = mapSession(index, session);
        if (prefixArray != null) {
            int slot = getSlot(session);
            Map<Long,
                Set<ISession<C>>> prefix2SessionMap = _Prefix2SessionMaps[slot];
            for (long prefix : prefixArray) {
                if (getSlot(prefix) != slot) {
                    throw new IllegalArgumentException(String.format("index: %#x, prefix: %#x | slot error",
                                                                     index,
                                                                     prefix));
                }
                prefix2SessionMap.computeIfAbsent(prefix, k -> new TreeSet<>())
                                 .add(session);
                session.bindPrefix(prefix);
            }
        }
        return old;
    }

    @Override
    public Collection<ISession<C>> clearAllSessionByPrefix(long prefix)
    {
        Map<Long,
            Set<ISession<C>>> prefix2SessionMap = _Prefix2SessionMaps[getSlot(prefix)];
        Set<ISession<C>> sessions = prefix2SessionMap.get(prefix);
        sessions.forEach(this::clearSession);
        return sessions;

    }

    @Override
    public ISession<C> clearSession(long index)
    {
        return _Index2SessionMaps[getSlot(index)].remove(index);
    }

    @Override
    public ISession<C> findSessionByIndex(long index)
    {
        return _Index2SessionMaps[getSlot(index)].get(index);
    }

    @Override
    public int getSessionCountByPrefix(long prefix)
    {
        return _Prefix2SessionMaps[getSlot(prefix)].size();
    }

    @Override
    public ISession<C> findSessionByPrefix(long prefix)
    {
        Set<ISession<C>> sessions = _Prefix2SessionMaps[getSlot(prefix)].get(prefix);
        Optional<ISession<C>> optional = sessions.stream()
                                                 .min(Comparator.comparing(session -> session.prefixLoad(prefix)));
        if (optional.isPresent()) {
            ISession<C> session = optional.get();
            session.prefixHit(prefix);
            return session;
        }
        return null;
    }

    public Collection<ISession<C>> findAllByPrefix(long prefix)
    {
        return _Prefix2SessionMaps[getSlot(prefix)].get(prefix);
    }
}
