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

package com.isahl.chess.bishop.io.mqtt.service;

import com.isahl.chess.bishop.io.mqtt.command.X113_QttPublish;
import com.isahl.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.isahl.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 */
public interface IQttStorage

{
    boolean brokerStorage(int msgId, String topic, byte[] content, long target);

    boolean deleteMessage(int msgId, long target);

    int generateMsgId(long target);

    void sessionOnLogin(long session, IQttRouter router, X111_QttConnect x111);

    void sessionOnSubscribe(long session, String topic, IQoS.Level level);

    void sessionOnUnsubscribe(long session, String topic);

    void cleanSession(long session);

    boolean hasReceived(int msgId, long origin);

    void receivedStorage(int msgId, String topic, byte[] content, long origin);

    X113_QttPublish takeStorage(int msgId, long origin);
}
