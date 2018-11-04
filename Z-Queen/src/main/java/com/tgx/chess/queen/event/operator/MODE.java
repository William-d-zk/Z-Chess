/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.queen.event.operator;

import static com.tgx.chess.queen.event.operator.OperatorHolder.*;

import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IOperatorSupplier;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.ISession;

public enum MODE
        implements
        IOperatorSupplier<IPacket, ICommand[], ISession>
{
    CLUSTER_CONSUMER
    {
    },
    CLUSTER_SERVER
    {
        @Override
        public IOperator<IPacket, ISession> getInOperator()
        {
            return CLUSTER_DECODER();
        }

        @Override
        public IOperator<ICommand[], ISession> getOutOperator()
        {
            return null;
        }
    },
    MQ_CONSUMER,
    MQ_SERVER,
    CONSUMER
    {
        @Override
        public IOperator<IPacket, ISession> getInOperator()
        {
            return CONSUMER_DECODER();
        }

        @Override
        public IOperator<ICommand[], ISession> getOutOperator()
        {
            return CONSUMER_TRANSFER();
        }
    },
    SERVER
    {
        @Override
        public IOperator<IPacket, ISession> getInOperator()
        {
            return SERVER_DECODER();
        }

        @Override
        public IOperator<ICommand[], ISession> getOutOperator()
        {
            return SERVER_TRANSFER();
        }
    },
    CONSUMER_SSL,
    SERVER_SSL,
    SYMMETRY;

    @Override
    public IOperator<IPacket, ISession> getInOperator()
    {
        return null;
    }

    @Override
    public IOperator<ICommand[], ISession> getOutOperator()
    {
        return null;
    }
}
