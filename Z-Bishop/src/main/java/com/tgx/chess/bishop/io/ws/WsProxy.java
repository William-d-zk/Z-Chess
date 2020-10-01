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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.                                                                      
 */

package com.tgx.chess.bishop.io.ws;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;

/**
 * @author william.d.zk
 */
public class WsProxy<C extends IContext<C>>
        implements
        IControl<ZContext>
{
    public final static int SERIAL = AioPacket.SERIAL + 1;

    private IControl<C>[] mBodies;

    @Override
    public int serial()
    {
        return SERIAL;
    }

    @Override
    public int superSerial()
    {
        return PACKET_SERIAL;
    }

    @SuppressWarnings("unchecked")
    public void add(IControl<C> body)
    {
        if (mBodies == null) {
            mBodies = new IControl[] { body };
        }
        else {
            IControl<C>[] nBodies = new IControl[mBodies.length + 1];
            IoUtil.addArray(mBodies, nBodies, body);
            mBodies = nBodies;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<C>[] getProxyBodies()
    {
        return mBodies;
    }

    @Override
    public boolean isProxy()
    {
        return true;
    }

    @Override
    public void setCtrl(byte ctrl)
    {

    }

    @Override
    public void setPayload(byte[] payload)
    {

    }

    @Override
    public byte[] getPayload()
    {
        return new byte[0];
    }

    @Override
    public byte getCtrl()
    {
        return 0;
    }

    @Override
    public boolean isCtrl()
    {
        return false;
    }

    @Override
    public void reset()
    {

    }

    @Override
    public int dataLength()
    {
        return 0;
    }

    @Override
    public Level getLevel()
    {
        return null;
    }
}
