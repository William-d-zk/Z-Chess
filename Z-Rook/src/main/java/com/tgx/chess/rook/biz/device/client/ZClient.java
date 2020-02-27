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

package com.tgx.chess.rook.biz.device.client;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IQoS;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZClient
{

    private final Queue<IControl<ZContext>>        _RecvMsgQueue = new LinkedList<>();
    private final Map<IControl<ZContext>,
                      Integer>                     _ConfirmMap   = new TreeMap<>(Comparator.comparing(IControl::getSequence));
    private String                                 clientId;
    private String                                 token;
    private String                                 username;
    private String                                 password;
    private short                                  localMsgId;
    private long                                   sequence;
    private long                                   sessionIndex;
    private String                                 sn;

    public ZClient()
    {
    }

    public void offer(IControl<ZContext> recv)
    {
        _RecvMsgQueue.offer(recv);
    }

    public IControl<ZContext> packet(IControl<ZContext> content)
    {
        switch (content.serial())
        {
            default:
                break;
        }
        content.setSequence(sequence++);
        IQoS.Level level = content.getLevel();
        if (level.getValue() > IQoS.Level.ALMOST_ONCE.getValue()) {
            _ConfirmMap.put(content, level.getValue());
        }
        return content;
    }

    public void confirm(IControl<ZContext> recv)
    {

    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public long getSessionIndex()
    {
        return sessionIndex;
    }

    public void setSessionIndex(long index)
    {
        sessionIndex = index;
    }

    public String getSn()
    {
        return sn;
    }

    public void setSn(String sn)
    {
        this.sn = sn;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public short getLocalMsgId()
    {
        return localMsgId;
    }

    public void setLocalMsgId(short localMsgId)
    {
        this.localMsgId = localMsgId;
    }

    public long getSequence()
    {
        return sequence;
    }

    public void setSequence(long sequence)
    {
        this.sequence = sequence;
    }
}
