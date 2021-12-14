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
package com.isahl.chess.queen.db.model;

import com.isahl.chess.board.base.ISerial;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author William.d.zk
 */
public interface IStorage
        extends ISerial
{
    long primaryKey();

    default long foreignKey()
    {
        return 0;
    }

    Operation operation();

    boolean hasForeignKey();

    Strategy strategy();

    enum Strategy
    {
        /**
         * 状态值需要进行持久化
         */
        RETAIN(Byte.parseByte("1")),
        /**
         * 仅与session 在内存中保持；
         * 数据状态保持与会话一致
         */
        CLEAN(Byte.parseByte("0")),
        INVALID(Byte.MIN_VALUE);

        final byte _Code;

        Strategy(byte code)
        {
            _Code = code;
        }

        public static Strategy valueOf(byte value)
        {
            return switch(value) {
                case 0 -> CLEAN;
                case 1 -> RETAIN;
                default -> INVALID;
            };
        }

        public byte getCode()
        {
            return _Code;
        }
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
        OP_INVALID(Byte.MIN_VALUE),// "10000000",Byte.parseByte 无法识别负数 2进制
        OP_FROZEN(Byte.parseByte("-127"));// "10000001"

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
            return _Value == value ? this : null;
        }

        public static Operation convertOperationValue(final byte value)
        {
            return Stream.of(Operation.values())
                         .map(op->op.predicate(value))
                         .filter(Objects::nonNull)
                         .findAny()
                         .orElse(OP_INVALID);
        }

        public static Operation valueOf(byte value)
        {
            return switch(value) {
                case 0 -> OP_NULL;
                case 1 -> OP_MODIFY;
                case 5 -> OP_INSERT;
                case 7 -> OP_APPEND;
                case 17 -> OP_REMOVE;
                case 25 -> OP_DELETE;
                case 32 -> OP_RESET;
                case 64 -> OP_RETRY;
                case -127 -> OP_FROZEN;
                default -> OP_INVALID;
            };
        }
    }
}
