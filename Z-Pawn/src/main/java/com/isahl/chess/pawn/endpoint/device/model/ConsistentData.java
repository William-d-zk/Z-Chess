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

package com.isahl.chess.pawn.endpoint.device.model;

import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.queen.message.InnerProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * @date 2022-01-01
 */
public class ConsistentData
        extends InnerProtocol
{
    class entry
    {
        long      peer;
        long      session;
        String    protocol;
        IoFactory factory;

        public entry(long session, long peer, String protocol, IoFactory factory)
        {
            this.peer = peer;
            this.session = session;
            this.protocol = protocol;
            this.factory = factory;
        }

        int size()
        {
            Objects.requireNonNull(protocol);
            return 8 + // sessionId
                   8 + // peerId
                   vSizeOf(protocol.length());// protocol.length
        }
    }

    private final Map<Long, entry> _Data = new HashMap<>();

    public void update(long session, long peerId, String protocol, IoFactory factory)
    {
        entry old = _Data.putIfAbsent(session, new entry(session, peerId, protocol, factory));
        old.session = session;
        old.peer = peerId;
        old.protocol = protocol;
        old.factory = factory;
    }

    public void remove(long session)
    {
        _Data.remove(session);
    }

    public IoFactory findFactoryBySessionId(long sessionId)
    {
        entry entry = _Data.get(sessionId);
        return entry == null ? null : entry.factory;
    }

    @Override
    public int length()
    {
        return 4 + // entry-size
               _Data.values()
                    .stream()
                    .mapToInt(entry::size)
                    .sum() + // entry-size sum
               super.length();
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putInt(_Data.size());
        for(entry e : _Data.values()) {
            output = output.putLong(e.session)
                           .putLong(e.peer)
                           .putUTF(e.protocol);
        }
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        int size = input.getInt();
        for(int i = 0; i < size; i++) {
            long session = input.getLong();
            long peer = input.getLong();
            int pl = input.vLength();
            String protocol = input.readUTF(pl);
            remain -= 8 + 8 + vSizeOf(pl);
            _Data.put(session,
                      new entry(session,
                                peer,
                                protocol,
                                ZSortHolder._Mapping(protocol)
                                           .getSort()
                                           .getFactory()));
        }
        return remain;
    }
}
