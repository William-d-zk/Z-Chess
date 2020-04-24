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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.bishop.io.zhandler;

import java.util.List;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.bishop.io.zprotocol.ztls.X02_AsymmetricPub;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X04_EncryptConfirm;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.bishop.io.zprotocol.ztls.X06_EncryptComp;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.handler.mix.ILinkCustom;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public class ZLinkMappingCustom
        implements
        ILinkCustom<ZContext>
{

    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    private final ILinkCustom<ZContext> _Then;

    public ZLinkMappingCustom(ILinkCustom<ZContext> then)
    {
        _Then = then;
    }

    @Override
    public IPair handle(ISessionManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> content) throws Exception
    {
        _Logger.info("link mapping receive %s", content);
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
                return new Pair<>(new IControl[] { content }, null);
            default:
                if (_Then == null) return null;
                return _Then.handle(manager, session, content);
        }
    }

    @Override
    public List<ITriple> notify(ISessionManager<ZContext> manager, IControl<ZContext> request, long origin)
    {
        return _Then != null ? _Then.notify(manager, request, origin)
                             : null;
    }
}
