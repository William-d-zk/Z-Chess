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

package com.tgx.chess.king.base.schedule;

import com.tgx.chess.king.base.schedule.inf.ITask;

/**
 * @author william.d.zk
 */
public enum Status
{

    MISS(-1 << ITask.RETRY_COUNT_BITS),
    CREATED(0)
    {
        @Override
        public boolean isTerminated()
        {
            return false;
        }
    },
    PENDING(1 << ITask.RETRY_COUNT_BITS)
    {
        @Override
        public boolean isTerminated()
        {
            return false;
        }
    },
    RUNNING(2 << ITask.RETRY_COUNT_BITS)
    {
        @Override
        public boolean isTerminated()
        {
            return false;
        }
    },
    STOP(3 << ITask.RETRY_COUNT_BITS),
    CANCEL((4 | 1) << ITask.RETRY_COUNT_BITS),
    COMPLETED((4 | 2) << ITask.RETRY_COUNT_BITS),
    ERROR(8 << ITask.RETRY_COUNT_BITS),
    TIME_OUT((16 | 8) << ITask.RETRY_COUNT_BITS),
    FAILED((32 | 8) << ITask.RETRY_COUNT_BITS);

    private final int _Code;

    public boolean isTerminated()
    {
        return true;
    }

    Status(int code)
    {
        _Code = code;
    }

    public int getCode()
    {
        return _Code;
    }

    public static Status valueOf(int code)
    {
        switch (code & ~ITask.RETRY_LIMIT)
        {
            case -1 << ITask.RETRY_COUNT_BITS:
                return MISS;
            case 0:
                return CREATED;
            case 1 << ITask.RETRY_COUNT_BITS:
                return PENDING;
            case 2 << ITask.RETRY_COUNT_BITS:
                return RUNNING;
            case 3 << ITask.RETRY_COUNT_BITS:
                return STOP;
            case (4 | 1) << ITask.RETRY_COUNT_BITS:
                return CANCEL;
            case (4 | 2) << ITask.RETRY_COUNT_BITS:
                return COMPLETED;
            case 8 << ITask.RETRY_COUNT_BITS:
                return ERROR;
            case (16 | 8) << ITask.RETRY_COUNT_BITS:
                return TIME_OUT;
            case (32 | 8) << ITask.RETRY_COUNT_BITS:
                return FAILED;
            default:
                throw new IllegalStateException("Unexpected value: " + (code & ~ITask.RETRY_LIMIT));
        }
    }
}
