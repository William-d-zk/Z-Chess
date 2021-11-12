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

package com.isahl.chess.knight.raft.model.replicate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.ITraceable;
import com.isahl.chess.queen.message.JsonProtocol;

/**
 * @author william.d.zk
 * @date 2020/7/13
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_CONSISTENT_SERIAL)
public class SnapshotEntry
        extends JsonProtocol
        implements ITraceable,
                   IStorage
{

    @Override
    public long primaryKey()
    {
        return 0;
    }

    @Override
    public Operation operation()
    {
        return null;
    }

    @Override
    public Strategy strategy()
    {
        return null;
    }

    @Override
    public int serial()
    {
        return 0;
    }

    @Override
    public long getOrigin()
    {
        return 0;
    }
}
