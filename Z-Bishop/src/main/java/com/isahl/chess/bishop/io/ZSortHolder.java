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

package com.isahl.chess.bishop.io;

import com.isahl.chess.bishop.io.mqtt.QttContext;
import com.isahl.chess.bishop.io.mqtt.filter.QttCommandFactory;
import com.isahl.chess.bishop.io.sort.mqtt.MqttZSort;
import com.isahl.chess.bishop.io.sort.websocket.WsZSort;
import com.isahl.chess.bishop.io.sort.websocket.proxy.WsProxyZSort;
import com.isahl.chess.bishop.io.sort.websocket.ssl.WsSslZSort;
import com.isahl.chess.bishop.io.zprotocol.ZClusterFactory;
import com.isahl.chess.bishop.io.zprotocol.ZServerFactory;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IPContext;

/**
 * @author william.d.zk
 * @date 2019-06-30
 */
public enum ZSortHolder
{
    WS_CONSUMER(new WsZSort(ISort.Mode.LINK, ISort.Type.CONSUMER)),
    WS_SERVER(new WsZSort(ISort.Mode.LINK, ISort.Type.SERVER)),
    WS_SERVER_SSL(new WsSslZSort(ISort.Mode.LINK, ISort.Type.SERVER, WS_SERVER.getSort())),
    WS_CLUSTER_SYMMETRY(new WsZSort(ISort.Mode.CLUSTER, ISort.Type.SYMMETRY)),
    WS_CLUSTER_CONSUMER(new WsZSort(ISort.Mode.CLUSTER, ISort.Type.CONSUMER)),
    WS_CLUSTER_SERVER(new WsZSort(ISort.Mode.CLUSTER, ISort.Type.SERVER)),
    QTT_SYMMETRY(new MqttZSort(ISort.Mode.LINK, ISort.Type.SYMMETRY)),
    QTT_SERVER(new MqttZSort(ISort.Mode.LINK, ISort.Type.SERVER)),
    WS_QTT_SERVER(new WsProxyZSort<QttContext>(ISort.Mode.LINK, ISort.Type.SERVER, QTT_SERVER.getSort()));

    private final ISort<?> _Sort;

    <C extends IPContext<C>> ZSortHolder(ISort<C> sort)
    {
        _Sort = sort;
    }

    public static IControl create(int serial)
    {
        if (serial > 0x110 && serial < 0x11F) { return _QttCommandFactory.create(serial); }
        if (serial > 0x1F && serial < 0x6F) { return _ServerFactory.create(serial); }
        if (serial >= 0x70 && serial <= 0x7F) { return _ClusterFactory.create(serial); }
        throw new IllegalArgumentException();
    }

    final static ZServerFactory    _ServerFactory     = new ZServerFactory();
    final static ZClusterFactory   _ClusterFactory    = new ZClusterFactory();
    final static QttCommandFactory _QttCommandFactory = new QttCommandFactory();

    @SuppressWarnings("unchecked")
    public <C extends IPContext<C>> ISort<C> getSort()
    {
        return (ISort<C>) _Sort;
    }
    /*
    
    
    final QttFrameFilter _QttFrameFilter = new QttFrameFilter();
    {
        _QttFrameFilter.linkAfter(new ZEFilter());
        _QttFrameFilter.linkFront(new QttControlFilter())
                       .linkFront(new QttCommandFilter());
    }
    final WsHandShakeFilter _WsHandshakeFilter = new WsHandShakeFilter();
    {
        IFilterChain<ZContext> header = new ZEFilter();
        _WsHandshakeFilter.linkAfter(header);
        _WsHandshakeFilter.linkFront(new WsFrameFilter())
                          .linkFront(new WsControlFilter())
                          .linkFront(new ZCommandFilter(new ZServerFactory()));
    }
    
    final WsFrameFilter _ClusterFrameFilter = new WsFrameFilter();
    {
        _ClusterFrameFilter.linkAfter(new ZEFilter());
        _ClusterFrameFilter.linkFront(new WsControlFilter())
                           .linkFront(new ZCommandFilter(new ZClusterFactory()));
    }
    
    final WsHandShakeFilter _QttWsHandshakeFilter = new WsHandShakeFilter();
    {
        QttFrameFilter mQttFrameFilter = new QttFrameFilter();
        mQttFrameFilter.linkFront(new QttControlFilter())
                       .linkFront(new QttCommandFilter());
        _QttWsHandshakeFilter.linkFront(new WsFrameFilter())
                             .linkFront(new WsControlFilter())
                             .linkFront(new WsProxyFilter(mQttFrameFilter));
    }
    
    public static ICommandFactory<? extends IControl<ZContext>,
                                  ? extends IFrame> getCommandFactory(int serial)
    {
        if (serial > 0x110 && serial < 0x11F) { return _QttCommandFactory; }
        if (serial > 0x1F && serial < 0x6F) { return _ServerFactory; }
        if (serial >= 0x70 && serial <= 0x7F) { return _ClusterFactory; }
        throw new IllegalArgumentException();
    }
    
     */
}
