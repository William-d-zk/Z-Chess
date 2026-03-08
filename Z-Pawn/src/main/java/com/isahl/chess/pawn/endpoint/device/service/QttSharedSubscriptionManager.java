/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * MQTT v5.0 共享订阅管理器
 * <p>
 * 支持 MQTT v5.0 的共享订阅特性（Shared Subscriptions）。
 * <p>
 * 共享订阅格式：$share/{group}/{topic}
 * <ul>
 *   <li>$share - 共享订阅前缀</li>
 *   <li>{group} - 组名，同一组的订阅者共享消息</li>
 *   <li>{topic} - 主题过滤器</li>
 * </ul>
 * <p>
 * 消息分发策略（Load Balancing）：
 * <ul>
 *   <li>轮询（Round Robin）- 默认</li>
 * </ul>
 *
 * @author william.d.zk
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Shared_subscriptions">MQTT v5.0 Shared Subscriptions</a>
 */
@Service
public class QttSharedSubscriptionManager
{
    private final static Logger _Logger = Logger.getLogger("endpoint.pawn." + QttSharedSubscriptionManager.class.getSimpleName());

    /**
     * 共享订阅前缀
     */
    public static final String SHARED_SUBSCRIPTION_PREFIX = "$share/";

    /**
     * 共享组映射 (groupName -> SharedGroup)
     */
    private final Map<String, SharedGroup> _Groups = new ConcurrentHashMap<>();

    /**
     * 会话到共享组的映射 (sessionId -> Set of groupNames)
     */
    private final Map<Long, Set<String>> _SessionToGroups = new ConcurrentHashMap<>();

    /**
     * 统计：共享订阅数量
     */
    private final AtomicInteger _SharedSubscriptionCount = new AtomicInteger(0);

    /**
     * 共享组
     */
    public static class SharedGroup
    {
        private final String                    _GroupName;
        private final String                    _TopicFilter;
        private final Pattern                   _TopicPattern;
        private final List<GroupMember>         _Members = new CopyOnWriteArrayList<>();
        private final AtomicInteger             _RoundRobinIndex = new AtomicInteger(0);

        public SharedGroup(String groupName, String topicFilter, Pattern topicPattern)
        {
            _GroupName = groupName;
            _TopicFilter = topicFilter;
            _TopicPattern = topicPattern;
        }

        public String getGroupName()
        {
            return _GroupName;
        }

        public String getTopicFilter()
        {
            return _TopicFilter;
        }

        public boolean matchesTopic(String topic)
        {
            return _TopicPattern.matcher(topic).matches();
        }

        /**
         * 添加组成员
         */
        public void addMember(long sessionId, IQoS.Level maxQoS)
        {
            // 检查是否已存在
            for (GroupMember member : _Members) {
                if (member.getSessionId() == sessionId) {
                    // 更新 QoS
                    member.setMaxQoS(maxQoS);
                    return;
                }
            }
            _Members.add(new GroupMember(sessionId, maxQoS));
            _Logger.debug("Added member to shared group [%s]: session=%#x, qos=%s", _GroupName, sessionId, maxQoS);
        }

        /**
         * 移除组成员
         */
        public void removeMember(long sessionId)
        {
            _Members.removeIf(member -> member.getSessionId() == sessionId);
            _Logger.debug("Removed member from shared group [%s]: session=%#x", _GroupName, sessionId);
        }

        /**
         * 检查是否为空
         */
        public boolean isEmpty()
        {
            return _Members.isEmpty();
        }

        /**
         * 获取成员数量
         */
        public int getMemberCount()
        {
            return _Members.size();
        }

        /**
         * 使用轮询策略选择下一个成员
         */
        public GroupMember selectNextMember()
        {
            if (_Members.isEmpty()) {
                return null;
            }
            int index = _RoundRobinIndex.getAndIncrement() % _Members.size();
            return _Members.get(index);
        }

        /**
         * 获取所有成员
         */
        public List<GroupMember> getAllMembers()
        {
            return Collections.unmodifiableList(_Members);
        }
    }

    /**
     * 组成员
     */
    public static class GroupMember
    {
        private final long          _SessionId;
        private       IQoS.Level    _MaxQoS;

        public GroupMember(long sessionId, IQoS.Level maxQoS)
        {
            _SessionId = sessionId;
            _MaxQoS = maxQoS;
        }

        public long getSessionId()
        {
            return _SessionId;
        }

        public IQoS.Level getMaxQoS()
        {
            return _MaxQoS;
        }

        public void setMaxQoS(IQoS.Level maxQoS)
        {
            _MaxQoS = maxQoS;
        }
    }

    // ==================== 共享订阅管理 ====================

    /**
     * 检查是否为共享订阅主题
     *
     * @param topicFilter 主题过滤器
     * @return true 如果是共享订阅
     */
    public boolean isSharedSubscription(String topicFilter)
    {
        return topicFilter != null && topicFilter.startsWith(SHARED_SUBSCRIPTION_PREFIX);
    }

    /**
     * 解析共享订阅
     *
     * @param sharedTopicFilter 共享订阅主题过滤器（$share/{group}/{topic}）
     * @return 解析结果 [groupName, topicFilter]，非法格式返回 null
     */
    public String[] parseSharedSubscription(String sharedTopicFilter)
    {
        if (!isSharedSubscription(sharedTopicFilter)) {
            return null;
        }

        String withoutPrefix = sharedTopicFilter.substring(SHARED_SUBSCRIPTION_PREFIX.length());
        int firstSlash = withoutPrefix.indexOf('/');
        if (firstSlash <= 0) {
            return null; // 非法格式
        }

        String groupName = withoutPrefix.substring(0, firstSlash);
        String topicFilter = withoutPrefix.substring(firstSlash + 1);

        if (topicFilter.isEmpty()) {
            return null; // 主题过滤器不能为空
        }

        return new String[]{groupName, topicFilter};
    }

    /**
     * 订阅共享主题
     *
     * @param sharedTopicFilter 共享订阅主题过滤器
     * @param sessionId         会话标识
     * @param maxQoS            最大 QoS
     * @return true 如果订阅成功
     */
    public boolean subscribe(String sharedTopicFilter, long sessionId, IQoS.Level maxQoS)
    {
        String[] parsed = parseSharedSubscription(sharedTopicFilter);
        if (parsed == null) {
            return false;
        }

        String groupName = parsed[0];
        String topicFilter = parsed[1];

        // 将主题过滤器转换为正则表达式
        Pattern topicPattern = topicFilterToRegex(topicFilter);

        // 获取或创建共享组
        SharedGroup group = _Groups.computeIfAbsent(groupName,
            k -> new SharedGroup(groupName, topicFilter, topicPattern));

        // 添加成员
        group.addMember(sessionId, maxQoS);

        // 记录会话到组的映射
        _SessionToGroups.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(groupName);

        _SharedSubscriptionCount.incrementAndGet();
        _Logger.info("Shared subscription added: session=%#x, group=%s, topic=%s, qos=%s",
            sessionId, groupName, topicFilter, maxQoS);

        return true;
    }

    /**
     * 取消共享订阅
     *
     * @param sharedTopicFilter 共享订阅主题过滤器
     * @param sessionId         会话标识
     * @return true 如果取消成功
     */
    public boolean unsubscribe(String sharedTopicFilter, long sessionId)
    {
        String[] parsed = parseSharedSubscription(sharedTopicFilter);
        if (parsed == null) {
            return false;
        }

        String groupName = parsed[0];

        SharedGroup group = _Groups.get(groupName);
        if (group == null) {
            return false;
        }

        // 移除成员
        group.removeMember(sessionId);
        _SharedSubscriptionCount.decrementAndGet();

        // 清理空组
        if (group.isEmpty()) {
            _Groups.remove(groupName);
            _Logger.debug("Shared group removed (empty): %s", groupName);
        }

        // 更新会话映射
        Set<String> groups = _SessionToGroups.get(sessionId);
        if (groups != null) {
            groups.remove(groupName);
            if (groups.isEmpty()) {
                _SessionToGroups.remove(sessionId);
            }
        }

        _Logger.info("Shared subscription removed: session=%#x, group=%s", sessionId, groupName);
        return true;
    }

    /**
     * 为消息选择订阅者
     * <p>使用轮询策略从匹配的共享组中选择一个成员</p>
     *
     * @param topic 主题名称
     * @return 选中的会话标识列表（每个匹配组选一个）
     */
    public List<SelectedSubscriber> selectSubscribers(String topic)
    {
        List<SelectedSubscriber> result = new ArrayList<>();

        for (SharedGroup group : _Groups.values()) {
            if (group.matchesTopic(topic)) {
                GroupMember member = group.selectNextMember();
                if (member != null) {
                    result.add(new SelectedSubscriber(
                        member.getSessionId(),
                        member.getMaxQoS(),
                        group.getGroupName()
                    ));
                }
            }
        }

        return result;
    }

    /**
     * 选中的订阅者
     */
    public static class SelectedSubscriber
    {
        private final long          _SessionId;
        private final IQoS.Level    _MaxQoS;
        private final String        _GroupName;

        public SelectedSubscriber(long sessionId, IQoS.Level maxQoS, String groupName)
        {
            _SessionId = sessionId;
            _MaxQoS = maxQoS;
            _GroupName = groupName;
        }

        public long getSessionId()
        {
            return _SessionId;
        }

        public IQoS.Level getMaxQoS()
        {
            return _MaxQoS;
        }

        public String getGroupName()
        {
            return _GroupName;
        }
    }

    /**
     * 获取会话的所有共享订阅组
     */
    public Set<String> getSessionGroups(long sessionId)
    {
        Set<String> groups = _SessionToGroups.get(sessionId);
        return groups != null ? Collections.unmodifiableSet(groups) : Collections.emptySet();
    }

    /**
     * 清理会话的所有共享订阅
     */
    public void clearSessionSubscriptions(long sessionId)
    {
        Set<String> groups = _SessionToGroups.remove(sessionId);
        if (groups == null) {
            return;
        }

        for (String groupName : groups) {
            SharedGroup group = _Groups.get(groupName);
            if (group != null) {
                group.removeMember(sessionId);
                _SharedSubscriptionCount.decrementAndGet();
                if (group.isEmpty()) {
                    _Groups.remove(groupName);
                }
            }
        }

        _Logger.debug("Cleared shared subscriptions for session: %#x", sessionId);
    }

    // ==================== 辅助方法 ====================

    /**
     * 将主题过滤器转换为正则表达式
     */
    private Pattern topicFilterToRegex(String topicFilter)
    {
        String regex = topicFilter
            .replace("+", "[^/]+")      // + 匹配单级
            .replace("#", ".*");         // # 匹配多级
        return Pattern.compile(regex);
    }

    // ==================== 统计信息 ====================

    /**
     * 获取共享组数量
     */
    public int getGroupCount()
    {
        return _Groups.size();
    }

    /**
     * 获取共享订阅数量
     */
    public int getSharedSubscriptionCount()
    {
        return _SharedSubscriptionCount.get();
    }

    /**
     * 获取统计信息
     */
    public String getStatistics()
    {
        return String.format(
            "SharedSubscriptionStats{groups=%d, subscriptions=%d}",
            getGroupCount(),
            getSharedSubscriptionCount()
        );
    }
}
