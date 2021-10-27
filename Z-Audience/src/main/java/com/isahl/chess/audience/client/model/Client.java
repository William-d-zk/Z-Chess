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

package com.isahl.chess.audience.client.model;

import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Client
{
    private final        Queue<IControl>        _RecvMsgQueue = new LinkedList<>();
    private final        Map<IControl, Integer> _ConfirmMap   = new TreeMap<>(Comparator.comparing(IControl::getSequence));
    private final        ZUID                   _ZUID;
    private final        Device                 _Device;
    private final static AtomicInteger          DEVICE_ID     = new AtomicInteger(1);

    public Client(ZUID zuid)
    {
        _ZUID = zuid;
        _Device = new Device(String.format("audience.device-for-test.%d", DEVICE_ID.getAndIncrement()));
    }

    public void offer(IControl recv)
    {
        _RecvMsgQueue.offer(recv);
    }

    public IControl packet(IControl content)
    {
        content.setSequence(_ZUID.getId(ZUID.TYPE_CONSUMER));
        IQoS.Level level = content.getLevel();
        if(level.getValue() > IQoS.Level.ALMOST_ONCE.getValue()) {
            _ConfirmMap.put(content, level.getValue());
        }
        return content;
    }

    public void confirm(IControl recv)
    {

    }

    public void setToken(String token)
    {
        _Device.setToken(token);
    }

    public long getClientId()
    {
        return _Device.getId();
    }

    public String getClientToken()
    {
        return _Device.getToken();
    }

    public String getUsername()
    {
        return _Device.getUsername();

    }

    public String getPassword()
    {
        return _Device.getPassword();
    }
}
