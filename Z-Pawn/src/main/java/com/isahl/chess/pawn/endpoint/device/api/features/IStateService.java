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

package com.isahl.chess.pawn.endpoint.device.api.features;

import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.MsgStateEntity;
import com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model.SessionEntity;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;

import java.util.Map;
import java.util.regex.Pattern;

public interface IStateService
{

    void store(long origin, long msgId, String topic, byte[] payload);

    MsgStateEntity query(long origin, long msgId);

    boolean exists(long origin, long msgId);

    void extract(long origin, long msgId);

    IThread.Subscribe onSubscribe(IThread.Topic topic, long session);

    void onUnsubscribe(IThread.Topic topic, long session);

    void add(long target, long msgId, String topic, byte[] payload);

    void drop(long target, long msgId);

    Map<Pattern, IThread.Subscribe> mappings();

    boolean onLogin(long session, boolean clean);

    /**
     * 重新登录时，主动清除历史状态| 离线时也需根据 clean-flag 进行清除
     *
     * @param session session-idx
     */
    void onDismiss(long session);

    SessionEntity load(long session);
}
