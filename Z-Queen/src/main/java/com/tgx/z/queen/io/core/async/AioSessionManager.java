/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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
package com.tgx.z.queen.io.core.async;

import static java.lang.System.arraycopy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tgx.z.king.base.log.Logger;
import com.tgx.z.king.base.util.ArrayUtil;
import com.tgx.z.king.config.Config;
import com.tgx.z.queen.config.QueenCode;
import com.tgx.z.queen.config.QueenConfigKey;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.core.inf.ISessionManager;

/**
 * @author William.d.zk
 */
public abstract class AioSessionManager
        implements
        ISessionManager
{
    protected static Logger               _Log                   = Logger.getLogger(AioSessionManager.class.getName());
    protected final Map<Long, ISession>[] _Index2SessionMaps     = new Map[4];
    protected final Map<Long, long[][]>[] _PortChannel2IndexMaps = new Map[4];
    protected final Set<ISession>[]       _SessionsSets          = new Set[4];
    // 每个mask都有单独对应的loadFair
    private final Map<Long, Integer>[]    _LoadFairMaps          = new Map[4];
    private final Config                  _Config;

    public Config getConfig() {
        return _Config;
    }

    private static String getConfigGroup() {
        return "io";
    }

    private static String getConfigName() {
        return "Io";
    }

    public AioSessionManager(Config config) {
        _Config = config.load(getConfigName());
        Arrays.setAll(_SessionsSets, slot -> new HashSet<>(1 << getConfigPower(config, slot)));
        Arrays.setAll(_Index2SessionMaps, slot -> new HashMap<>(1 << getConfigPower(config, slot)));
        Arrays.setAll(_PortChannel2IndexMaps, slot -> new HashMap<>(23));
    }

    private int getConfigPower(Config config, int slot) {
        int power;
        switch (slot) {
            case CLIENT_SLOT:
                power = config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_QUEEN_POWER, QueenConfigKey.KEY_POWER_CLIENT);
                break;
            case INTERNAL_SLOT:
                power = config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_QUEEN_POWER, QueenConfigKey.KEY_POWER_INTERNAL);
                break;
            case SERVER_SLOT:
                power = config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_QUEEN_POWER, QueenConfigKey.KEY_POWER_SERVER);
                break;
            case CLUSTER_SLOT:
                power = config.getConfigValue(getConfigGroup(), QueenConfigKey.OWNER_QUEEN_POWER, QueenConfigKey.KEY_POWER_CLUSTER);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return power;
    }

    protected int getSlot(ISession session) {
        return (int) ((session.getIndex() & QueenCode.XID_MK) >>> 62);
    }

    protected int getSlot(long index) {
        return (int) ((index & QueenCode.XID_MK) >>> 62);
    }

    public void addSession(ISession session) {
        int slot = getSlot(session);
        _Log.info(String.format("add session ->set slot:%s",
                                slot == CLIENT_SLOT ? "CLIENT"
                                                    : slot == INTERNAL_SLOT ? "INTERNAL" : slot == SERVER_SLOT ? "SERVER" : "CLUSTER"));
        _SessionsSets[slot].add(session);
    }

    @Override
    public void rmSession(ISession session) {
        _SessionsSets[getSlot(session)].remove(session);
    }

    /**
     * @return 正常情况下返回 _Index 返回 NULL_INDEX 说明 Map 失败。 或返回被覆盖的 OLD-INDEX 需要对其进行
     *         PortChannel 的清理操作。
     */
    @Override
    public boolean mapSession(long _Index, ISession session) {
        if (_Index == INVALID_INDEX || _Index == NULL_INDEX) return false;
        /*
         * 1:相同 Session 不同 _Index 进行登录，产生多个 _Index 对应 相同 Session 的情况 2:相同 _Index 在不同的 Session 上登录，产生覆盖 Session 的情况。
         */
        long sOldIndex = session.getIndex();
        if (sOldIndex != INVALID_INDEX) _Index2SessionMaps[getSlot(session)].remove(sOldIndex);
        // 检查可能覆盖的 Session 是否存在,_Index 已登录过
        ISession oldSession = _Index2SessionMaps[getSlot(session)].put(_Index, session);
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
                }
                // 相同 Session 上相同 _Index 重复登录
            }
            // 被覆盖的 Session 持有不同的 _Index
            else if (oldIndex != INVALID_INDEX) {
                ISession oldMappedSession = _Index2SessionMaps[getSlot(session)].get(oldIndex);
                /*
                 * oldIndex bind oldSession 已在 Map 完成其他的新的绑定关系。
                 */
                if (oldMappedSession == oldSession) _Log.debug("oldMappedSession == oldSession -> Ignore");// Ignore
                else if (oldMappedSession == null) _Log.debug("oldMappedSession == null -> oldIndex invalid");// oldIndex 已失效
                // else oldIndex 已完成其他的绑定过程无需做任何处理。
            }
        }
        return true;
    }

    @Override
    public boolean mapSession(long index, ISession session, long... portIdArray) {
        if (mapSession(index, session)) {
            if (portIdArray != null) for (int size = portIdArray.length, lv = size - 1; lv > -1; lv--) {
                long portId = portIdArray[lv];
                if (portId == INVALID_INDEX || portId == NULL_INDEX) continue;
                long[][] c = _PortChannel2IndexMaps[getSlot(session)].get(portId);
                boolean put = c == null;
                if (put) c = new long[2][];
                if (c[0] == null) c[0] = new long[2];
                _Index:
                {
                    int i = 0, cSize = c[0].length;
                    for (; i < cSize; i++) {
                        if (c[0][i] == index) break _Index;
                        else if (c[0][i] == 0 || i == cSize - 2) break;
                    }
                    long o = c[0][i];
                    if (o != NULL_INDEX) {
                        long[] t = new long[cSize + 1];
                        arraycopy(c[0], 0, t, 1, cSize);
                        t[cSize] = cSize;
                        t[0] = index;
                        c[0] = t;
                    }
                    else {
                        c[0][i] = index;
                        c[0][cSize - 1] += 1;
                    }
                }
                _SubFilter:
                {
                    if (lv > 0 && (portIdArray[lv - 1] & portId) != 0) {
                        if (c[1] == null) c[1] = new long[2];
                        int i = 0, cSize = c[1].length;
                        for (; i < cSize; i++) {
                            if (c[1][i] == portIdArray[lv - 1]) break _SubFilter;
                            else if (c[1][i] == NULL_INDEX || i == cSize - 1) break;
                        }
                        long o = c[1][i];
                        if (o != 0) {
                            long[] t = new long[cSize + 2];
                            arraycopy(c[1], 0, t, 1, cSize);
                            t[0] = portIdArray[lv - 1];
                            c[1] = t;
                        }
                        else c[1][i] = portIdArray[lv - 1];
                    }
                }
                session.bindport2channel(portId);
                if (put) _PortChannel2IndexMaps[getSlot(session)].put(portId, c);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clearSessionWithPort(long index, ISession session) {
        // 未登录链接不在map管理范畴内，操作的_Index不一致也不行
        if (index != session.getIndex() && session.getIndex() != INVALID_INDEX || index == INVALID_INDEX || index == NULL_INDEX) return;
        _Index2SessionMaps[getSlot(session)].remove(index);
        long[] _cPortChannel = session.getPortChannels();
        long rmf = NULL_INDEX;
        if (_cPortChannel != null) for (long filter : _cPortChannel) {
            long[][] c = _PortChannel2IndexMaps[getSlot(session)].get(filter);
            if (c != null && c.length > 0 && c[0] != null) {
                _Index:
                {
                    long[] a = c[0];
                    int k = -1, j = -1;
                    for (int i = 0; i < a.length - 1; i++) {
                        if (a[i] == index) k = i;
                        else if (a[i] == NULL_INDEX) {
                            j = i;
                            if (k >= 0) break;
                        }
                    }
                    if (k < 0) break _Index;
                    if (j < 0) j = a.length - 2;
                    for (int t = k; t < j; t++)
                        ArrayUtil.swap(a, t, t + 1);
                    a[j] = 0;
                    a[a.length - 1] -= 1;
                    if (a[a.length - 1] == 0) {
                        c[0] = null;
                        if (rmf == 0) rmf = filter;
                    }
                }

            }
            if (c != null && c.length > 1 && c[1] != null && rmf != NULL_INDEX) {
                _SubFilter:
                {
                    long[] a = c[1];
                    int k = -1, j = -1;
                    for (int i = 0; i < a.length; i++) {
                        if (a[i] == rmf) k = i;
                        else if (a[i] == NULL_INDEX) {
                            j = i;
                            if (k < 0) continue;
                            else break;
                        }
                    }
                    if (k < 0) break _SubFilter;
                    if (j < 0) j = a.length - 1;
                    for (int t = k; t < j; t++)
                        ArrayUtil.swap(a, t, t + 1);
                    a[j] = NULL_INDEX;
                }
                rmf = NULL_INDEX;
            }
        }
    }

    @Override
    public void clearSession(long index) {
        _Index2SessionMaps[getSlot(index)].remove(index);
    }

    @Override
    public ISession findSessionByIndex(long index) {
        return _Index2SessionMaps[getSlot(index)].get(index);
    }

    @Override
    public boolean findPort(long portMask) {
        return _PortChannel2IndexMaps[getSlot(portMask)].containsKey(portMask);
    }

    @Override
    public int getSubPortCount(long portMask) {
        long[][] c = _PortChannel2IndexMaps[getSlot(portMask)].get(portMask);
        int size = 0;
        if (c != null && c.length > 1 && c[1] != null && c[1].length > 1) {
            for (long l : c[1]) {
                if (l == NULL_INDEX) continue;
                ISession session = findSessionByPortLoadFairKeepOld(l);
                if (session != null && !session.isClosed()) size++;
            }
        }
        return size;
    }

    @Override
    public int getPortCount(long portMask) {
        long[][] c = _PortChannel2IndexMaps[getSlot(portMask)].get(portMask);
        if (c != null && c.length > 0 && c[0] != null && c[0].length > 1) {
            ISession session = null;
            int size = (int) c[0][c[0].length - 1];
            _Log.info("port-size: " + size);
            if (size == 0) return 0;
            else {
                int result = 0;
                for (int i = 0; i < size; i++) {
                    long index = c[0][i];
                    if (index == NULL_INDEX) continue;
                    session = _Index2SessionMaps[getSlot(index)].get(index);
                    if (session == null || session.isClosed()) continue;
                    result++;
                }
                return result;
            }
        }
        return 0;
    }

    @Override
    public long[][] portIndex(long portMask) {
        return _PortChannel2IndexMaps[getSlot(portMask)].get(portMask);
    }

    // 防止同种类型的连接在数量上造成困扰，如每个cm连接4个cm，每个cm之间有4个Session，每次调用会得到同一session
    // 获取Session,但是对应的loadFair不增长，防止因getPortSessionCount而造成loadFair的自增
    private ISession findSessionByPortLoadFairKeepOld(long portMask) {
        int loadFairOld = _LoadFairMaps[getSlot(portMask)].get(portMask) == null ? 0 : _LoadFairMaps[getSlot(portMask)].get(portMask);
        ISession aioSession = findSessionByPort(portMask);
        int loadFairAfter = _LoadFairMaps[getSlot(portMask)].get(portMask) == null ? 0 : _LoadFairMaps[getSlot(portMask)].get(portMask);
        if (loadFairAfter != 0) _LoadFairMaps[getSlot(portMask)].put(portMask, loadFairOld);
        return aioSession;
    }

    @Override
    public ISession findSessionByPort(long portMask) {
        long[][] c = _PortChannel2IndexMaps[getSlot(portMask)].get(portMask);
        if (c != null && c.length > 0 && c[0] != null && c[0].length > 1) {
            ISession session;
            int size = (int) c[0][c[0].length - 1];
            _Log.info("port-size: " + size);
            if (size == 0) return null;
            else if (size == 1) {
                long index = c[0][0];
                if (index == 0) return null;
                session = _Index2SessionMaps[getSlot(index)].get(index);
                if (session == null || session.isClosed()) return null;
                return session;
            }
            else {
                int loadFair = _LoadFairMaps[getSlot(portMask)].get(portMask) == null ? 0 : _LoadFairMaps[getSlot(portMask)].get(portMask);
                long idx = c[0][(loadFair++ & Integer.MAX_VALUE) % size];
                _LoadFairMaps[getSlot(portMask)].put(portMask, loadFair);
                _Log.info("session index: "
                          + Long.toHexString(idx)
                                .toUpperCase()
                          + "load fair: "
                          + loadFair);

                session = _Index2SessionMaps[getSlot(idx)].get(idx);
                if (session == null || session.isClosed()) {
                    for (int i = 0; i < size; i++) {
                        long index = c[0][i];
                        if (index == 0) continue;
                        session = _Index2SessionMaps[getSlot(index)].get(index);
                        if (session != null && !session.isClosed()) return session;
                    }
                }
                else return session;
            }
        }
        return null;
    }

    //    @Override
    //    public Collection<Long> getIndexSet() {
    //        return _Index2SessionMaps.keySet();
    //    }
    //
    //    public ISession[] getSessions() {
    //        ISession[] x = new ISession[_SessionsSets.size()];
    //        _SessionsSets.toArray(x);
    //        return x;
    //    }
    //
    //    @Override
    //    public Iterator<ISession> iterator() {
    //        return _SessionsSets.iterator();
    //    }
    //
    //    @Override
    //    public final int sessionCount() {
    //        return _SessionsSets.size();
    //    }
    //
    //    public List<ICommand> heartbeatAll(List<ICommand> wList, ICommand heartbeat) {
    //        for (ISession session : _Index2SessionMaps.values())
    //            wList.add(heartbeat.duplicate()
    //                               .setSession(session));
    //        return wList;
    //    }

}
