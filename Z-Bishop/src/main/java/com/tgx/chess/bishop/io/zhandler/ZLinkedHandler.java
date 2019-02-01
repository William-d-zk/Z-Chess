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

package com.tgx.chess.bishop.io.zhandler;

import static com.tgx.chess.queen.event.inf.IError.Type.SAVE_DATA;

import com.tgx.chess.bishop.io.device.X20_SignUp;
import com.tgx.chess.bishop.io.device.X22_SignIn;
import com.tgx.chess.bishop.io.device.X24_UpdateToken;
import com.tgx.chess.bishop.io.ztls.X01_EncryptRequest;
import com.tgx.chess.bishop.io.ztls.X02_AsymmetricPub;
import com.tgx.chess.bishop.io.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.ztls.X04_EncryptConfirm;
import com.tgx.chess.bishop.io.ztls.X05_EncryptStart;
import com.tgx.chess.bishop.io.ztls.X06_PlainStart;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.handler.ILinkHandler;
import com.tgx.chess.queen.event.handler.LinkHandler;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;

public class ZLinkedHandler
        implements
        ILinkHandler
{

    private static Logger LOG = Logger.getLogger(ZLinkedHandler.class.getName());

    @Override
    public void handle(LinkHandler _LinkHandler, QueenManager manager, QEvent event)
    {

        Pair<ICommand,
             ISession> logicContent = event.getContent();
        LOG.info("LinkHandler cmd: %s", logicContent.first());
        ISession session = logicContent.second();
        ICommand cmd = logicContent.first();
        ICommand[] waitToSends = null;
        IOperator<ICommand[],
                  ISession> sendOperator = session.getHandler()
                                                  .getOutOperator();
        switch (cmd.getSerial())
        {
            case X01_EncryptRequest.COMMAND:
            case X02_AsymmetricPub.COMMAND:
            case X03_Cipher.COMMAND:
            case X04_EncryptConfirm.COMMAND:
            case X05_EncryptStart.COMMAND:
            case X06_PlainStart.COMMAND:
                waitToSends = new ICommand[] { cmd };
                break;
            case X20_SignUp.COMMAND:
                try {
                    waitToSends = new ICommand[] { manager.save(cmd, session) };
                }
                catch (Exception e) {
                    _LinkHandler.error(SAVE_DATA, e, session);
                }
                break;
            case X22_SignIn.COMMAND:
                try {
                    waitToSends = new ICommand[] { manager.find(cmd, session) };
                }
                catch (Exception e) {
                    _LinkHandler.error(SAVE_DATA, e, session);
                }
                break;
            case X24_UpdateToken.COMMAND:
                try {
                    waitToSends = new ICommand[] { manager.save(cmd, session) };
                }
                catch (Exception e) {
                    _LinkHandler.error(SAVE_DATA, e, session);
                }
                break;
        }
        _LinkHandler.write(waitToSends, session, sendOperator);
    }
}
