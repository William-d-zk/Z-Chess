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
package com.isahl.chess.queen.io.core.inf;

/**
 * @author William.d.zk
 */
public interface IQoS
        extends
        ISequence,
        Comparable<IQoS>
{
    int QOS_PRIORITY_00_NETWORK_CONTROL    = 4 << 0;
    int QOS_PRIORITY_01_CLUSTER_CONTROL    = 4 << 1;
    int QOS_PRIORITY_02_MQ_CONTROL         = 4 << 2;
    int QOS_PRIORITY_03_CLUSTER_EXCHANGE   = 4 << 3;
    int QOS_PRIORITY_04_MQ_EXCHANGE        = 4 << 4;
    int QOS_PRIORITY_05_SYNC_MODIFY        = 4 << 5;
    int QOS_PRIORITY_06_META_CREATE        = 4 << 6;
    int QOS_PRIORITY_07_ROUTE_MESSAGE      = 4 << 7;
    int QOS_PRIORITY_08_IMMEDIATE_MESSAGE  = 4 << 8;
    int QOS_PRIORITY_09_CONFIRM_MESSAGE    = 4 << 9;
    int QOS_PRIORITY_10_QUERY_MESSAGE      = 4 << 10;
    int QOS_PRIORITY_11_MQ_MODIFY          = 4 << 11;
    int QOS_PRIORITY_12_PUSH_MESSAGE       = 4 << 12;
    int QOS_PRIORITY_13_POSTPONE_MESSAGE   = 4 << 13;
    int QOS_PRIORITY_14_NO_CONFIRM_MESSAGE = 4 << 14;
    int QOS_PRIORITY_15_INNER_CMD          = 4 << 15;

    int getPriority();

    @Override
    default int compareTo(IQoS o)
    {
        long seqDiff      = getSequence() - o.getSequence();
        int  priorityDiff = getPriority() - o.getPriority();
        return priorityDiff == 0 ?
                (seqDiff == 0 ?
                        hashCode() - o.hashCode():
                        (seqDiff > 0 ?
                                1:
                                -1)):
                priorityDiff;
    }

    Level getLevel();

    /**
     * 数据传输质量等级
     */
    enum Level
    {

        /**
         * 只传输1次
         * 不确保送达
         * 类似UDP
         */
        ALMOST_ONCE(0),
        /**
         * 最多传输成功1次
         * 确保送达
         * 但不确认接收方是否完成处理
         * 近似TCP
         */
        AT_LEAST_ONCE(1),
        /**
         * 至少成功传输1次
         * 确保送达
         * 确认接收方完成接收处理
         * 至少成功传输一次
         */
        EXACTLY_ONCE(2),
        /**
         * 故障
         */
        FAILURE(0x80);

        final int _Value;

        Level(int value)
        {
            _Value = value;
        }

        public int getValue()
        {
            return _Value;
        }

        public static Level valueOf(int level)
        {
            return switch (level) {
                case 0 -> ALMOST_ONCE;
                case 1 -> AT_LEAST_ONCE;
                case 2 -> EXACTLY_ONCE;
                case 0x80 -> FAILURE;
                default -> throw new UnsupportedOperationException();
            };
        }

    }

}
