/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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

package com.tgx.chess.config;

import java.time.Duration;

import org.springframework.util.unit.DataSize;

import com.tgx.chess.queen.config.ISocketConfig;

public class SocketConfig
        implements
        ISocketConfig
{
    private boolean  keepAlive;
    private Duration writeTimeOutInSecond;
    private Duration readTimeOutInMinute;
    private Duration soLingerInSecond;
    private DataSize sendBufferSize;
    private DataSize recvBufferSize;
    private int      sendQueueMax;
    private boolean  tcpNoDelay;

    @Override
    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    @Override
    public Duration getWriteTimeOutInSecond()
    {
        return writeTimeOutInSecond;
    }

    @Override
    public Duration getReadTimeOutInMinute()
    {
        return readTimeOutInMinute;
    }

    @Override
    public int getSendQueueMax()
    {
        return sendQueueMax;
    }

    @Override
    public int getRcvInByte()
    {
        return (int) recvBufferSize.toBytes();
    }

    @Override
    public int getSnfInByte()
    {
        return (int) sendBufferSize.toBytes();
    }

    @Override
    public boolean isTcpNoDelay()
    {
        return tcpNoDelay;
    }

    @Override
    public Duration getSoLingerInSecond()
    {
        return soLingerInSecond;
    }

    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }

    public void setWriteTimeOutInSecond(Duration writeTimeOutInSecond) {
        this.writeTimeOutInSecond = writeTimeOutInSecond;
    }

    public void setReadTimeOutInMinute(Duration readTimeOutInMinute) {
        this.readTimeOutInMinute = readTimeOutInMinute;
    }

    public void setSoLingerInSecond(Duration soLingerInSecond) {
        this.soLingerInSecond = soLingerInSecond;
    }

    public void setSendBufferSize(DataSize sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public void setRecvBufferSize(DataSize recvBufferSize) {
        this.recvBufferSize = recvBufferSize;
    }

    public void setSendQueueMax(int sendQueueMax) {
        this.sendQueueMax = sendQueueMax;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
}
