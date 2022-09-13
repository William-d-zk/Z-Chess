/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

/**
 * @author william.d.zk
 * @date 2019-08-04
 */
public interface IRouter
{

    /**
     * register message with state for session by id
     *
     * @param msgId 需要等待ack的消息 持有的msg-id
     * @param body  消息本体
     */
    <P extends IRoutable & IProtocol & IQoS> void register(long msgId, P body);

    /**
     * feed back message state
     *
     * @param msgId  响应的msg-id
     * @param origin 消息来源的session-index
     * @return 是否对消息完成了ack动作
     */
    boolean ack(long msgId, long origin);

    /**
     * clean session state machine for message stack
     *
     * @param session session index
     */
    void clean(long session);

}
