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

package com.isahl.chess.pawn.endpoint.device.db.remote.postgres.service;

import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.repository.IMessageJpaRepository;
import com.isahl.chess.pawn.endpoint.device.spi.plugin.PersistentHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author william.d.zk
 */
@Service
public class MessageStorageService
        implements PersistentHook.ISubscribe
{
    private final IMessageJpaRepository _JpaRepository;

    @Autowired
    public MessageStorageService(IMessageJpaRepository repository)
    {
        _JpaRepository = repository;
    }

    @Override
    public void onBatch(List<IoSerial> contents)
    {
        for(IoSerial content : contents) {
            if(content instanceof MessageEntity msg) {
                _JpaRepository.save(msg);
            }
        }
    }
}
