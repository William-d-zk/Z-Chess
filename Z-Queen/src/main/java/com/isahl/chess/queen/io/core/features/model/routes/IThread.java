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

package com.isahl.chess.queen.io.core.features.model.routes;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.BinarySerial;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;
import static java.lang.String.format;

/**
 * @author william.d.zk
 * @date 2022-01-10
 */
public interface IThread
{

    /**
     * 内存是足够大的，所以每个节点都存储全量的关联表
     * 然后通过session-prefix进行集群节点路由
     *
     * @param topic 主题
     * @return
     */
    List<Subscribe.Mapped> broker(String topic);

    /**
     * @param topic    主题
     * @param retained 持续待发,任何订阅主题的 session 都会在首次完成订阅时收到retained 信息
     */
    void retain(String topic, IProtocol retained);

    /**
     * @param topic   主题
     * @param session 订阅的 session
     * @return
     */
    Subscribe subscribe(Topic topic, long session);

    /**
     * @param topic   主题
     * @param session 取消主题的session
     */
    void unsubscribe(Topic topic, long session);

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    class Topic
            extends BinarySerial
    {
        @Serial
        private static final long serialVersionUID = -6998834630245751766L;

        private Pattern    mPattern;
        private IQoS.Level mLevel;
        private int        mAlias;
        private boolean    mRetain;

        public Topic(Pattern pattern, IQoS.Level level, int alias)
        {
            Objects.requireNonNull(pattern, "Topic's pattern is null");
            Objects.requireNonNull(level, "topic level is null");
            mPattern = pattern;
            mLevel = level;
            mAlias = alias;
        }

        public Topic(Pattern pattern)
        {
            Objects.requireNonNull(pattern, "Topic's pattern is null");
            mPattern = pattern;
            mLevel = IQoS.Level.ALMOST_ONCE;
            mAlias = 0;
        }

        public Topic(Pattern pattern, IQoS.Level level)
        {
            Objects.requireNonNull(pattern, "Topic's pattern is null");
            Objects.requireNonNull(level, "topic level is null");
            mPattern = pattern;
            mLevel = level;
            mAlias = 0;
        }

        public Topic(ByteBuf input) {super(input);}

        public Pattern pattern() {return mPattern;}

        public IQoS.Level level() {return mLevel;}

        public int alias() {return mAlias;}

        public boolean retain() {return mRetain;}

        public void keep()
        {
            mRetain = true;
        }

        @Override
        public int length()
        {
            int length = 4 + // alias
                         1;  // level
            length += vSizeOf(mPattern.pattern()
                                      .getBytes(StandardCharsets.UTF_8).length);
            return super.length() + length;
        }

        @Override
        public int prefix(ByteBuf input)
        {
            int remain = super.prefix(input);
            int b = input.get();
            mRetain = (b & IQoS.Level._N_MASK) != 0;
            mLevel = IQoS.Level.valueOf(b & IQoS.Level._MASK);
            mAlias = input.getInt();
            int pl = input.vLength();
            if(pl > 0) {
                String pattern = input.readUTF(pl);
                mPattern = Pattern.compile(pattern);
            }
            remain -= 5 + vSizeOf(pl);
            return remain;
        }

        @Override
        public ByteBuf suffix(ByteBuf output)
        {
            return super.suffix(output)
                        .put(mLevel.getValue() | (mRetain ? IQoS.Level._N_MASK : 0))
                        .putInt(mAlias)
                        .vPut(mPattern != null ? mPattern.pattern()
                                                         .getBytes(StandardCharsets.UTF_8) : null);
        }

        @Override
        public String toString()
        {
            return format("Topic{ pattern:%s level:%s alais:%d with-retain:%s}", pattern(), level(), alias(), retain());
        }

        @Override
        public boolean equals(Object o)
        {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Topic topic = (Topic) o;

            if(mAlias != topic.mAlias) return false;
            if(!mPattern.equals(topic.mPattern)) return false;
            return mLevel == topic.mLevel;
        }

        @Override
        public int hashCode()
        {
            int result = mPattern.hashCode();
            result = 31 * result + mLevel.hashCode();
            result = 31 * result + mAlias;
            return result;
        }
    }

    class Subscribe
    {
        public record Mapped(long session, IQoS.Level level) {}

        private final Map<Long, IQoS.Level> _SessionMap = new ConcurrentSkipListMap<>();
        private final Pattern               _Pattern;
        private       IProtocol             mRetained;

        public Subscribe(Pattern pattern) {_Pattern = pattern;}

        public void onDismiss(long session)
        {
            _SessionMap.remove(session);
        }

        public void setRetain(IProtocol content)
        {
            mRetained = content;
        }

        public Pattern pattern()
        {
            return _Pattern;
        }

        public IProtocol retained()
        {
            return mRetained;
        }

        public void onSubscribe(long session, IQoS.Level level)
        {
            _SessionMap.put(session, level);
        }

        public List<Mapped> mapped()
        {
            return _SessionMap.entrySet()
                              .stream()
                              .map(e->new Mapped(e.getKey(), e.getValue()))
                              .collect(Collectors.toList());
        }

        public Stream<Mapped> stream()
        {
            return _SessionMap.entrySet()
                              .stream()
                              .map(e->new Mapped(e.getKey(), e.getValue()));

        }

        public void from(List<Mapped> other)
        {
            other.forEach(mapped->_SessionMap.put(mapped.session(), mapped.level()));
        }

        public boolean isEmpty()
        {
            return _SessionMap.isEmpty();
        }

        public IQoS.Level computeIfPresent(Long session, BiFunction<Long, IQoS.Level, IQoS.Level> remapping)
        {
            return _SessionMap.computeIfPresent(session, remapping);
        }

        public boolean contains(long session)
        {
            return _SessionMap.containsKey(session);
        }

        public IQoS.Level level(long session)
        {
            return _SessionMap.get(session);
        }
    }

}
