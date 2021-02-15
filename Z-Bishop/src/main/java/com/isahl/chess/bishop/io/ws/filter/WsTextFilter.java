/*
 * MIT License                                                                     
 *                                                                                 
 * Copyright (c) 2016~2021. Z-Chess                                                
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

package com.isahl.chess.bishop.io.ws.filter;

import static com.isahl.chess.bishop.io.ws.WsFrame.frame_op_code_no_ctrl_json;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.bishop.io.ws.IWsContext;
import com.isahl.chess.bishop.io.ws.WsFrame;
import com.isahl.chess.bishop.io.zprotocol.control.X10A_Json;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.IFrame;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2021/2/14
 */
public class WsJsonFilter<T extends ZContext & IWsContext>
        extends
        AioFilterChain<T,
                       ICommand,
                       IFrame>
{
    public WsJsonFilter()
    {
        super("ws_json");
    }

    @Override
    public WsFrame encode(T context, ICommand output)
    {
        WsFrame frame = new WsFrame();
        frame.setPayload(output.getPayload());
        frame.setCtrl(output.getCtrl());
        return frame;
    }

    @Override
    public ICommand decode(T context, IFrame input)
    {
        ICommand command = new X10A_Json();
        byte ctrl = input.getCtrl();
        if (ctrl != frame_op_code_no_ctrl_json) {
            _Logger.warning("input should be formatted by json");
        }
        command.setPayload(input.getPayload());
        return command;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType,
                                      IPContext> pipeSeek(IPContext context, O output)
    {
        if (checkType(output, IProtocol.COMMAND_SERIAL)
            && context instanceof IWsContext
            && context.isOutConvert()
            && !context.isProxy())
        {
            return new Pair<>(ResultType.NEXT_STEP, context);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType,
                                      IPContext> pipePeek(IPContext context, I input)
    {
        if (checkType(input, IProtocol.FRAME_SERIAL)
            && !((IFrame) input).isCtrl()
            && context.isInConvert()
            && context instanceof IWsContext
            && !context.isProxy())
        {
            return new Pair<>(ResultType.HANDLED, context);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol,
            I extends IProtocol> I pipeEncode(IPContext context, O output)
    {

        return (I) encode((T) context, (ICommand) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol,
            I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (IFrame) input);
    }

}
