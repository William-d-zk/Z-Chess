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

import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.bishop.io.zprotocol.ztls.X02_AsymmetricPub;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X04_EncryptConfirm;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.bishop.io.zprotocol.ztls.X06_PlainStart;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.event.handler.ILinkHandler;
import com.tgx.chess.queen.event.handler.LinkHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author william.d.zk
 */
public class ZLinkedHandler
        implements
        ILinkHandler<ZContext>
{

    private final Logger _Logger = Logger.getLogger(getClass().getName());

    @Override
    @SuppressWarnings("unchecked")
    public void handle(LinkHandler<ZContext> _LinkHandler, QueenManager<ZContext> manager, QEvent event)
    {
        IPair logicContent = event.getContent();
        ISession<ZContext> session = logicContent.second();
        IControl<ZContext> cmd = logicContent.first();
        _Logger.info("LinkHandler cmd: %s", cmd);
        IControl<ZContext>[] waitToSends = null;
        switch (cmd.getSerial())
        {
            case X01_EncryptRequest.COMMAND:
            case X02_AsymmetricPub.COMMAND:
            case X03_Cipher.COMMAND:
            case X04_EncryptConfirm.COMMAND:
            case X05_EncryptStart.COMMAND:
            case X06_PlainStart.COMMAND:
                /*
                 *  内嵌逻辑，在ZCommandFilter中已经处理结束
                 *  此处仅执行转发逻辑
                 */
                waitToSends = new IControl[] { cmd };
                break;
            case X111_QttConnect.COMMAND:
                try {
                    waitToSends = manager.handle(cmd, session);
                }
                catch (Exception e) {
                    _LinkHandler.error(SAVE_DATA,
                                       e,
                                       session,
                                       session.getContext()
                                              .getSort()
                                              .getError());
                    return;
                }
                break;
            default:
                break;
        }
        _LinkHandler.write(waitToSends, session);
    }
}
