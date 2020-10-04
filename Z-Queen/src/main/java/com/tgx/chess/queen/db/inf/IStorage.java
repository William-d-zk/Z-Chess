/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.queen.db.inf;

import java.util.Objects;
import java.util.stream.Stream;

import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public interface IStorage
        extends
        IProtocol
{
    @Override
    default int superSerial()
    {
        return DB_SERIAL;
    }

    long primaryKey();

    Operation operation();

    Strategy strategy();

    enum Strategy
    {
        /**
         * 状态值需要进行持久化
         */
        RETAIN,
        /**
         * 会话状态不保持
         * 每次声明会话都清除之前的状态。
         */
        CLEAN
    }

    enum Operation
    {
        OP_NULL(Byte.parseByte("00000000", 2)),
        OP_MODIFY(Byte.parseByte("00000001", 2)),
        OP_INSERT(Byte.parseByte("00000101", 2)),
        OP_APPEND(Byte.parseByte("00000111", 2)),
        OP_REMOVE(Byte.parseByte("00010001", 2)),
        OP_DELETE(Byte.parseByte("00011001", 2)),
        OP_RESET(Byte.parseByte("00100000", 2)),
        OP_RETRY(Byte.parseByte("01000000", 2)),
        OP_INVALID(Byte.MIN_VALUE),
        OP_FROZEN(Byte.parseByte("-127"));

        private final byte _Value;

        Operation(byte value)
        {
            _Value = value;
        }

        public byte getValue()
        {
            return _Value;
        }

        public Operation predicate(byte value)
        {
            return _Value == value ?
                    this:
                    null;
        }

        public static Operation convertOperationValue(final byte value)
        {
            return Stream.of(Operation.values())
                         .map(op -> op.predicate(value))
                         .filter(Objects::nonNull)
                         .findAny()
                         .orElse(OP_INVALID);
        }

    }

}
