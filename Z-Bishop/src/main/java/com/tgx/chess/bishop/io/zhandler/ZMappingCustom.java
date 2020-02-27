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

package com.tgx.chess.bishop.io.zhandler;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.bishop.io.zprotocol.ztls.X02_AsymmetricPub;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X04_EncryptConfirm;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.bishop.io.zprotocol.ztls.X06_EncryptComp;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.event.inf.ICustomLogic;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;

public class ZMappingCustom
        implements
        ICustomLogic<ZContext>
{

    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    private final ICustomLogic<ZContext> _Then;

    public ZMappingCustom(ICustomLogic<ZContext> andThen)
    {
        _Then = andThen;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<ZContext>[] handle(QueenManager<ZContext> manager,
                                       ISession<ZContext> session,
                                       IControl<ZContext> content) throws Exception
    {
        _Logger.info("mapping %s", content);
        switch (content.serial())
        {
            case X01_EncryptRequest.COMMAND:
            case X02_AsymmetricPub.COMMAND:
            case X03_Cipher.COMMAND:
            case X04_EncryptConfirm.COMMAND:
            case X05_EncryptStart.COMMAND:
            case X06_EncryptComp.COMMAND:
                /*
                 *  内嵌逻辑，在ZCommandFilter中已经处理结束
                 *  此处仅执行转发逻辑
                 */
                return new IControl[] { content };
            default:
                if (_Then == null) return null;
                return _Then.handle(manager, session, content);
        }
    }
}
