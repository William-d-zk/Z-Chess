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

package com.tgx.chess.bishop.io.zoperator;

import com.tgx.chess.bishop.io.zprotocol.Command;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X22_SignIn;
import com.tgx.chess.bishop.io.zprotocol.device.X23_SignInResult;
import com.tgx.chess.bishop.io.zprotocol.device.X24_UpdateToken;
import com.tgx.chess.bishop.io.zprotocol.device.X25_AuthorisedToken;
import com.tgx.chess.bishop.io.zprotocol.device.X30_EventMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X31_ConfirmMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X32_MsgStatus;
import com.tgx.chess.bishop.io.zprotocol.device.X50_DeviceMsg;
import com.tgx.chess.bishop.io.zprotocol.device.X51_DeviceMsgAck;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;

public enum ZCommandFactories
        implements
        ICommandFactory<ZContext,
                        Command<ZContext>>
{

    SERVER
    {

        @Override
        public Command<ZContext> create(int command)
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
                    LOG.warning(String.format("server command is not Handled : %x", command));
                    return null;
            }

        }
    },
    CLUSTER
    {
        @Override
        public Command<ZContext> create(int command)
        {
            LOG.warning(String.format("cluster command is not Handled : %x", command));
            return null;
        }
    },
    MQ
    {
        @Override
        public Command<ZContext> create(int command)
        {
            LOG.warning(String.format("mq-service command is not Handled : %x", command));
            return null;
        }
    },
    CONSUMER
    {
        public Command<ZContext> create(int command)
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
                    LOG.warning(String.format("consumer command is not Handled : %x", command));
                    return null;
            }

        }

    };
    private static Logger LOG = Logger.getLogger(ZCommandFactories.class.getName());
}
