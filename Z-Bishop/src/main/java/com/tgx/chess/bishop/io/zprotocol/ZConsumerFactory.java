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

package com.tgx.chess.bishop.io.zprotocol;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.device.*;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public interface ZConsumerFactory
        extends
        ICommandFactory<ZContext,
                        ZCommand>
{
    @Override
    default ZCommand create(int command)
    {
        return consumerCommand(command);
    }

    default ZCommand consumerCommand(int command)
    {
        switch (command)
        {
            case X21_SignUpResult.COMMAND:
                return new X21_SignUpResult();
            case X23_SignInResult.COMMAND:
                return new X23_SignInResult();
            case X25_AuthorisedToken.COMMAND:
                return new X25_AuthorisedToken();
            case X30_EventMsg.COMMAND:
                return new X30_EventMsg();
            case X32_MsgStatus.COMMAND:
                return new X32_MsgStatus();
            case X51_DeviceMsgAck.COMMAND:
                return new X51_DeviceMsgAck();
            default:
                return null;
        }
    }
}
