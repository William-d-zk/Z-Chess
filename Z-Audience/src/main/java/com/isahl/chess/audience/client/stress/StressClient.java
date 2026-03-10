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

package com.isahl.chess.audience.client.stress;

import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 压测客户端封装 - 简化版
 */
public class StressClient implements IValid
{
    private final long clientId;
    private final PressureTestConfig config;
    private final PressureMetrics metrics;
    private final TimeWheel timeWheel;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ICancelable heartbeatTask;
    private volatile ICancelable requestTask;
    private ISession session;
    private java.util.function.Consumer<StressClient> onConnectedCallback;
    private java.util.function.Consumer<StressClient> onDisconnectedCallback;
    private java.util.function.Consumer<Throwable> onErrorCallback;

    public StressClient(long clientId, PressureTestConfig config, PressureMetrics metrics, TimeWheel timeWheel)
    {
        this.clientId = clientId;
        this.config = config;
        this.metrics = metrics;
        this.timeWheel = timeWheel;
    }

    public void onConnected(ISession session)
    {
        this.session = session;
        connected.set(true);
    }

    public void onConnectFailed(Exception e)
    {
        connected.set(false);
    }

    public void onDisconnected(ISession session)
    {
        connected.set(false);
        this.session = null;
    }

    public void startSending()
    {
        running.set(true);
    }

    public void stop()
    {
        running.set(false);
    }

    public void close()
    {
        stop();
        if(heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        if(requestTask != null) {
            requestTask.cancel();
        }
        if(session != null) {
            try {
                session.close();
            } catch(Exception e) {
                // ignore
            }
        }
    }

    public boolean isConnected()
    {
        return connected.get();
    }

    public int getPendingRequestCount()
    {
        return 0;
    }

    public void onResponse(long requestId, byte[] data)
    {
        // 简化实现
    }

    public long getClientId()
    {
        return clientId;
    }

    public void setOnConnectedCallback(java.util.function.Consumer<StressClient> callback)
    {
        this.onConnectedCallback = callback;
    }

    public void setOnDisconnectedCallback(java.util.function.Consumer<StressClient> callback)
    {
        this.onDisconnectedCallback = callback;
    }

    public void setOnErrorCallback(java.util.function.Consumer<Throwable> callback)
    {
        this.onErrorCallback = callback;
    }
}
