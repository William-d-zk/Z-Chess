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

package com.isahl.chess.pawn.endpoint.device.spi;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IExchanger;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

/**
 * @author william.d.zk
 */
public interface IAccessService
{
    boolean isSupported(IoSerial input);

    boolean isSupported(ISession session);

    /**
     * @param exchanger session manager
     * @param session   by session
     * @param content   input IProtocol
     * @param load      handle on 'load' element: fst 「IControl」snd「ISession」trd「ISession.IEncoder」
     */
    void onLogic(IExchanger exchanger, ISession session, IProtocol content, List<ITriple> load);

    /**
     * @param manager session manager
     * @param session input session
     * @param input   input IProtocol
     * @return ITriple
     * fst: 直接在session上需要进行返回的内容「IProtocol/List of IProtocol」
     * snd: 需要向Cluster进行一致性处理的内容「IProtocol」
     * trd: 处理模式 fst 对应的内容需要以什么方式处理「 SINGLE/BATCH 」
     */
    ITriple onLink(IManager manager, ISession session, IProtocol input);

    /**
     * @param session close session
     * @return session , 如果非空返回，说明是当前节点管理的 session
     */
    default ISession clean(IManager manager, long session)
    {
        return manager.cleanIndex(session);
    }

    List<ITriple> onConsistency(IManager manager, IConsistency backload, IoSerial consensusBody);

    default void consume(IExchanger exchanger, IoSerial request, List<ITriple> load)
    {

    }

    default void onExchange(IProtocol request, List<ITriple> load)
    {

    }

    default IProtocol onClose(ISession session)
    {
        return null;
    }
}
