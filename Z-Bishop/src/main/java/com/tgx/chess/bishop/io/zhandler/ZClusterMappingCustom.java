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

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.tgx.chess.bishop.io.mqtt.control.X11C_QttPingreq;
import com.tgx.chess.bishop.io.mqtt.control.X11D_QttPingresp;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.ws.control.X105_Pong;
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
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.IClusterCustom;
import com.tgx.chess.queen.io.core.inf.IConsistentProtocol;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public class ZClusterMappingCustom<T extends IStorage>
        implements
        IClusterCustom<ZContext,
                       T>
{
    private final Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getSimpleName());

    private final IClusterCustom<ZContext,
                                 T> _Then;

    public ZClusterMappingCustom(IClusterCustom<ZContext,
                                                T> then)
    {
        _Then = then;
    }

    @Override
    public IPair handle(ISessionManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> content) throws Exception
    {
        _Logger.debug("cluster mapping receive %s", content);
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
            case X104_Ping.COMMAND:
                X104_Ping x104 = (X104_Ping) content;
                _Logger.info("recv ping");
                return new Pair<>(new IControl[] { new X105_Pong(String.format("pong:%#x %s",
                                                                               session.getIndex(),
                                                                               session.getLocalAddress())
                                                                       .getBytes(StandardCharsets.UTF_8)) },
                                  null);
            case X105_Pong.COMMAND:
            case X11D_QttPingresp.COMMAND:
                return null;
            case X11C_QttPingreq.COMMAND:
                return new Pair<>(new IControl[] { new X11D_QttPingresp() }, null);
            default:
                if (_Then == null) return null;
                return _Then.handle(manager, session, content);
        }
    }

    @Override
    public List<ITriple> onTimer(ISessionManager<ZContext> manager, T content)
    {
        return _Then != null ? _Then.onTimer(manager, content)
                             : null;
    }

    @Override
    public List<ITriple> consensus(ISessionManager<ZContext> manager, IConsistentProtocol request, long origin)
    {
        return _Then != null ? _Then.consensus(manager, request, origin)
                             : null;
    }

    @Override
    public boolean waitForCommit()
    {
        return _Then != null && _Then.waitForCommit();
    }
}
