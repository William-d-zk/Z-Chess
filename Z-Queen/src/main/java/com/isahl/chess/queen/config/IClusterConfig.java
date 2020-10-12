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

package com.isahl.chess.queen.config;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/23
 */
public interface IClusterConfig
{
    /**
     * pipeline decoder 分配的固定配额
     *
     * @return decoder count power
     */
    int getDecoderCountPower();

    /**
     * pipeline encoder 分配的固定配额
     * 
     * @return encoder count power
     */
    int getEncoderCountPower();

    /**
     * pipeline 逻辑处理单元数量
     *
     * @return logic count power
     */
    int getLogicCountPower();

    /**
     * 用于处理集群通讯的处理器单元数
     *
     * @return cluster io-count power
     */
    int getClusterIoCountPower();

    /**
     * AIO 处理队列的阶乘数
     *
     * @return aio queue size power
     */
    int getAioQueueSizePower();

    /**
     * Cluster相关处理队列的阶乘数
     *
     * @return cluster queue size power
     */
    int getClusterQueueSizePower();

    /**
     * 逻辑处理单元的处理队列阶乘数
     *
     * @return logic queue size power
     */
    int getLogicQueueSizePower();

    /**
     * 异常处理单元的处理队列阶乘数
     *
     * @return error queue size power
     */
    int getErrorQueueSizePower();

    /**
     * 处理主动关闭时间的队列阶乘数
     *
     * @return closer queue size power
     */
    int getCloserQueueSizePower();

    /**
     * 
     * @return thread pool size
     */
    default int getPoolSize()
    {
        return 1 // io-dispatch
               + (1 << getDecoderCountPower()) // read-decode
               + 1 // decoded-dispatch
               + 1 // logic-processor
               + 1 // cluster-single
               + (1 << getLogicCountPower()) // notify-processor
               + 1 // write-dispatch
               + (1 << getEncoderCountPower())// write-encode
               + 1 // encoded-processor[write-end]
        ;
    }
}
