/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.queen.config;

/**
 * 由于主Pipeline 使用了disruptor 的 RingBuffer
 * 所以有些参数直接使用 __Power 直接设定RingBuffer的 _SIZE
 * 
 * @author william.d.zk
 * @date 2017/02/10
 */
public interface IMixConfig
        extends
        IClusterConfig
{

    /**
     * 用户处理订阅服务的处理单元数
     *
     * @return
     */
    int getBizIoCount();

    /**
     * Local 相关处理队列的阶乘数
     *
     * @return
     */
    int getLocalPower();

    /**
     * Link 相关处理队列的阶乘数
     *
     * @return
     */
    int getLinkPower();

    default int getPoolSize()
    {
        return 1 // aioDispatch
               + getDecoderCount() // read-decode
               + 1 // cluster-single
               + 1 // link-single
               + getLogicCount()// logic-
               + 1 // decoded-dispatch
               + 1 // write-dispatch
               + getEncoderCount()// write-encode
               + 1 // write-end
        ;
    }

}
