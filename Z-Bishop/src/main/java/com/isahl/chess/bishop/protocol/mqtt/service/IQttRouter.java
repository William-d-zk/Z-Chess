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

package com.isahl.chess.bishop.protocol.mqtt.service;

import com.isahl.chess.queen.io.core.features.model.routes.IRouter;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.QttControl;
import com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

/**
 * @author william.d.zk
 * @date 2019-07-09
 */
public interface IQttRouter
        extends IRouter
{

    void retain(String topic, QttControl msg);

    /**
     * 向特定主题发送消息
     *
     * @param topic
     * @return
     */
    Map<Long, IQoS.Level> broker(final String topic);

    /**
     * 注册单个主题
     *
     * @param topic
     * @param level
     * @param session
     * @return
     */
    Subscribe subscribe(String topic, IQoS.Level level, long session);

    /**
     * 注销主题
     *
     * @param topic
     * @param session
     */
    void unsubscribe(String topic, long session);

    class Subscribe
    {
        public final Map<Long, IQoS.Level> _SessionMap = new ConcurrentSkipListMap<>();
        public final Pattern     _Pattern;
        public       QttProtocol mRetained;

        public Subscribe(Pattern pattern) {_Pattern = pattern;}

        public void remove(long session)
        {
            _SessionMap.remove(session);
        }

    }

}
