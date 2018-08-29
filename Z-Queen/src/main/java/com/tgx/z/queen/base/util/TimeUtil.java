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
package com.tgx.z.queen.base.util;

import com.tgx.z.config.QueenCode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author William.d.zk
 */
public class TimeUtil
{
    public final static long        ZERO_AFTER_UTC_DELTA      = 1483228800000L;
    private static final AtomicLong ATOMIC_SEQUENCE_OF_SECOND = new AtomicLong(0);
    public static long              CURRENT_TIME_MILLIS_CACHE = System.currentTimeMillis();
    public static long              CURRENT_TIME_SECOND_CACHE = TimeUnit.MILLISECONDS.toSeconds(CURRENT_TIME_MILLIS_CACHE);
    private static long             SEQUENCE_21_OF_SECOND, SEQUENCE_20_OF_SECOND;
    private static long             SEQUENCE_201_OF_SECOND;

    public static long getUID16YearCollision2M() {
        long second = CURRENT_TIME_SECOND_CACHE - ZERO_AFTER_UTC_DELTA;
        return QueenCode.UID_TIME_27_MK & (second << 21) | (++SEQUENCE_21_OF_SECOND & QueenCode.UID_SEQ_21_MK);
    }

    public static long getUID16Year2TypeCollision1M() {
        long second = CURRENT_TIME_SECOND_CACHE - ZERO_AFTER_UTC_DELTA;
        return QueenCode.UID_TIME_27_MK & (second << 21) | ((++SEQUENCE_201_OF_SECOND << 1) & QueenCode.UID_SEQ_20_1_MK);
    }

    public static long getUID32YearCollision1M() {
        long second = CURRENT_TIME_SECOND_CACHE - ZERO_AFTER_UTC_DELTA;
        return QueenCode.UID_TIME_28_MK & (second << 20) | (++SEQUENCE_20_OF_SECOND & QueenCode.UID_SEQ_20_MK);
    }

    public static long concurrentGetUID16YearCollision2M() {
        long second = CURRENT_TIME_SECOND_CACHE - ZERO_AFTER_UTC_DELTA;
        return QueenCode.UID_TIME_27_MK & (second << 21) | (ATOMIC_SEQUENCE_OF_SECOND.incrementAndGet() & QueenCode.UID_SEQ_21_MK);
    }

    public static long concurrentGetUID32YearCollision1M() {
        long second = CURRENT_TIME_SECOND_CACHE - ZERO_AFTER_UTC_DELTA;
        return QueenCode.UID_TIME_28_MK & (second << 20) | (ATOMIC_SEQUENCE_OF_SECOND.incrementAndGet() & QueenCode.UID_SEQ_20_MK);
    }

    public static long concurrentGetUID16Year2TypeCollision1M() {
        long second = CURRENT_TIME_SECOND_CACHE - ZERO_AFTER_UTC_DELTA;
        return QueenCode.UID_TIME_27_MK & (second << 21) | ((ATOMIC_SEQUENCE_OF_SECOND.incrementAndGet() << 1) & QueenCode.UID_SEQ_20_1_MK);
    }

    public static String printTime(long millisecond) {
        OffsetDateTime instant = Instant.ofEpochMilli(millisecond)
                                        .atOffset(ZoneOffset.ofHours(8));
        return instant.toString();
    }

}
