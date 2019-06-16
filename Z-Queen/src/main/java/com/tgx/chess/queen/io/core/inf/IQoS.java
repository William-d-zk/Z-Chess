/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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
package com.tgx.chess.queen.io.core.inf;

/**
 * @author William.d.zk
 */
public interface IQoS
        extends
        ISequence,
        Comparable<IQoS>
{
    int QOS_00_NETWORK_CONTROL    = 1 << 0;
    int QOS_01_CLUSTER_CONTROL    = 1 << 1;
    int QOS_02_MQ_CONTROL         = 1 << 2;
    int QOS_03_CLUSTER_EXCHANGE   = 1 << 3;
    int QOS_04_MQ_EXCHANGE        = 1 << 4;
    int QOS_05_SYNC_MODIFY        = 1 << 5;
    int QOS_06_META_CREATE        = 1 << 6;
    int QOS_07_ROUTE_MESSAGE      = 1 << 7;
    int QOS_08_IMMEDIATE_MESSAGE  = 1 << 8;
    int QOS_09_CONFIRM_MESSAGE    = 1 << 9;
    int QOS_10_QUERY_MESSAGE      = 1 << 10;
    int QOS_11_MQ_MODIFY          = 1 << 11;
    int QOS_12_PUSH_MESSAGE       = 1 << 12;
    int QOS_13_POSTPONE_MESSAGE   = 1 << 13;
    int QOS_14_NO_CONFIRM_MESSAGE = 1 << 14;
    int QOS_15_INNER_CMD          = 1 << 15;

    int getPriority();

    @Override
    default int compareTo(IQoS o)
    {
        long seqDiff = getSequence() - o.getSequence();
        int priorityDiff = getPriority() - o.getPriority();
        return priorityDiff == 0 ? (seqDiff == 0 ? hashCode() - o.hashCode()
                                                 : (seqDiff > 0 ? 1
                                                                : -1))
                                 : priorityDiff;
    }

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
        ONLY_ONCE,
        /**
         * 最多传输成功1次
         * 确保送达
         * 但不确认接收方是否完成处理
         * 近似TCP
         */
        AT_LEAST_ONCE,
        /**
         * 至少成功传输1次
         * 确保送达
         * 确认接收方完成接收处理
         * 至少成功传输一次
         */
        LESS_ONCE;
    }

}
