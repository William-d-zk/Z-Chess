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

package com.tgx.chess.queen.event.inf;

import com.lmax.disruptor.EventHandler;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;

import java.util.Objects;

import static com.tgx.chess.queen.event.inf.IOperator.Type.*;

public interface ILogicHandler<C extends IContext<C>>
        extends
        IPipeEventHandler<QEvent>,
        ICustomLogic<C>
{

    QueenManager<C> getQueenManager();

    @Override
    default void onEvent(QEvent event, long sequence, boolean endOfBatch)
    {
        if (event.getEventType() == LOGIC) {
            IControl<C> content = event.getContent()
                                       .first();
            ISession<C> session = event.getContent()
                                       .second();
            if (content != null) {
                try {
                    IControl<C>[] response = handle(getQueenManager(), session, content);
                    if (Objects.nonNull(response) && response.length > 0) {
                        event.produce(LOGIC,
                                      new Pair<>(response, session),
                                      session.getContext()
                                             .getSort()
                                             .getTransfer());
                    }
                    else {
                        event.ignore();
                    }
                }
                catch (Exception e) {
                    event.error(IError.Type.HANDLE_DATA,
                                new Pair<>(e, session),
                                session.getContext()
                                       .getSort()
                                       .getCloser());
                }
            }
            else {
                event.ignore();
            }
        }
    }
}
