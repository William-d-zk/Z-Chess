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

package com.isahl.chess.player.api.mock;

import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.config.CodeKing;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.knight.raft.service.RaftPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;

@RestController
@RequestMapping("mock")
public class MockApi
{
    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    final RaftPeer _Peer;

    @Autowired
    public MockApi(RaftPeer peer) {_Peer = peer;}

    @GetMapping("mapper")
    public @ResponseBody ZResponse<?> mapper()
    {
        mockEntryInput().forEach(entry->{
            _Peer.mapper()
                 .append(entry);
        });
        _Peer.mapper()
             .flush();
        return ZResponse.of(CodeKing.SUCCESS, "-", "mapper test");
    }

    private List<LogEntry> mockEntryInput()
    {
        long term = 1;
        long index = MIN_START;
        List<LogEntry> logs = new ArrayList<>();
        for(long size = 10 + MIN_START; index <= size; index++) {
            logs.add(new LogEntry(index,
                                  term,
                                  0xC002000000000000L,
                                  0x6079376BC6400L,
                                  QttFactory._Instance.serial(),
                                  new byte[]{ (byte) 0x82,
                                              0x66,
                                              (byte) 0xE2,
                                              0x00,
                                              0x04,
                                              0x74,
                                              0x65,
                                              0x73,
                                              0x74,
                                              0x00 }));
        }
        return logs;
    }
}
