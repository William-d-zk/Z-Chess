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

package com.isahl.chess.bishop.io.zprotocol;

import com.isahl.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.isahl.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.isahl.chess.bishop.io.zprotocol.device.X24_UpdateToken;
import com.isahl.chess.bishop.io.zprotocol.device.X31_ConfirmMsg;
import com.isahl.chess.bishop.io.zprotocol.device.X32_MsgStatus;
import com.isahl.chess.bishop.io.zprotocol.device.X50_DeviceMsg;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.ICommandFactory;
import com.isahl.chess.queen.io.core.inf.IFrame;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-08
 */
public class ZServerFactory
        implements
        ICommandFactory<ICommand,
                        IFrame>
{

    @Override
    public ICommand create(IFrame frame)
    {
        return create(frame.getPayload()[1] & 0xFF);
    }

    @Override
    public ICommand create(int serial)
    {
        return switch (serial)
        {
            case X20_SignUp.COMMAND -> new X20_SignUp();
            case X22_SignIn.COMMAND -> new X22_SignIn();
            case X24_UpdateToken.COMMAND -> new X24_UpdateToken();
            case X31_ConfirmMsg.COMMAND -> new X31_ConfirmMsg();
            case X32_MsgStatus.COMMAND -> new X32_MsgStatus();
            case X50_DeviceMsg.COMMAND -> new X50_DeviceMsg();
            default -> throw new UnsupportedOperationException();
        };
    }

}
