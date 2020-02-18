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
public interface ZServerFactory
        extends
        ICommandFactory<ZContext,
                        ZCommand>
{

    @Override
    default ZCommand create(int command)
    {
        return serverCommand(command);
    }

    default ZCommand serverCommand(int command)
    {
        switch (command)
        {
            case X20_SignUp.COMMAND:
                return new X20_SignUp();
            case X22_SignIn.COMMAND:
                return new X22_SignIn();
            case X24_UpdateToken.COMMAND:
                return new X24_UpdateToken();
            case X31_ConfirmMsg.COMMAND:
                return new X31_ConfirmMsg();
            case X32_MsgStatus.COMMAND:
                return new X32_MsgStatus();
            case X50_DeviceMsg.COMMAND:
                return new X50_DeviceMsg();
            default:
                return null;
        }
    }
}
