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

package com.isahl.chess.bishop.io.sort;

import com.isahl.chess.bishop.io.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.io.mqtt.ctrl.X11E_QttDisconnect;
import com.isahl.chess.bishop.io.mqtt.factory.QttFactory;
import com.isahl.chess.bishop.io.sort.mqtt.MqttZSort;
import com.isahl.chess.bishop.io.sort.ssl.SslZSort;
import com.isahl.chess.bishop.io.sort.websocket.WsTextZSort;
import com.isahl.chess.bishop.io.sort.websocket.proxy.WsProxyZSort;
import com.isahl.chess.bishop.io.ws.zchat.WsZSort;
import com.isahl.chess.bishop.io.ws.zchat.ZlsZSort;
import com.isahl.chess.bishop.io.ws.zchat.factory.ZClusterFactory;
import com.isahl.chess.bishop.io.ws.zchat.factory.ZServerFactory;
import com.isahl.chess.bishop.io.ws.zchat.model.command.raft.X70_RaftVote;
import com.isahl.chess.bishop.io.ws.zchat.model.command.raft.X77_RaftNotify;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.net.socket.features.IAioSort;

import static com.isahl.chess.king.env.ZUID.*;

/**
 * @author william.d.zk
 * @date 2019-06-30
 */
public enum ZSortHolder
{
    WS_ZCHAT_CONSUMER(new WsZSort(ISort.Mode.LINK, ISort.Type.CLIENT)),
    WS_ZCHAT_SERVER(new WsZSort(ISort.Mode.LINK, ISort.Type.SERVER)),
    WS_ZCHAT_CONSUMER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                         ISort.Type.CLIENT,
                                         new WsZSort(ISort.Mode.LINK, ISort.Type.CLIENT))),
    WS_ZCHAT_SERVER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                       ISort.Type.SERVER,
                                       new WsZSort(ISort.Mode.LINK, ISort.Type.SERVER))),
    WS_CLUSTER_SYMMETRY(new WsZSort(ISort.Mode.CLUSTER, ISort.Type.SYMMETRY)),
    WS_CLUSTER_SYMMETRY_ZLS(new ZlsZSort(ISort.Mode.CLUSTER,
                                         ISort.Type.SYMMETRY,
                                         new WsZSort(ISort.Mode.CLUSTER, ISort.Type.SYMMETRY))),
    WS_PLAIN_TEXT_SERVER(new WsTextZSort(ISort.Mode.LINK, ISort.Type.SERVER)),
    WS_PLAIN_TEXT_SERVER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                            ISort.Type.SERVER,
                                            new WsTextZSort(ISort.Mode.LINK, ISort.Type.SERVER))),
    QTT_CONSUMER(new MqttZSort(ISort.Mode.LINK, ISort.Type.CLIENT)),
    QTT_SERVER(new MqttZSort(ISort.Mode.LINK, ISort.Type.SERVER)),
    QTT_SYMMETRY(new MqttZSort(ISort.Mode.LINK, ISort.Type.SYMMETRY)),
    QTT_CONSUMER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                    ISort.Type.CLIENT,
                                    new MqttZSort(ISort.Mode.LINK, ISort.Type.CLIENT))),
    QTT_SERVER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                  ISort.Type.SERVER,
                                  new MqttZSort(ISort.Mode.LINK, ISort.Type.SERVER))),
    QTT_SYMMETRY_SSL(new SslZSort<>(ISort.Mode.LINK,
                                    ISort.Type.SYMMETRY,
                                    new MqttZSort(ISort.Mode.LINK, ISort.Type.SYMMETRY))),
    WS_QTT_CONSUMER(new WsProxyZSort<>(ISort.Mode.LINK,
                                       ISort.Type.CLIENT,
                                       new MqttZSort(ISort.Mode.LINK, ISort.Type.CLIENT))),
    WS_QTT_CONSUMER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                       ISort.Type.CLIENT,
                                       new WsProxyZSort<>(ISort.Mode.LINK,
                                                          ISort.Type.CLIENT,
                                                          new MqttZSort(ISort.Mode.LINK, ISort.Type.CLIENT)))),
    WS_QTT_SERVER(new WsProxyZSort<>(ISort.Mode.LINK,
                                     ISort.Type.SERVER,
                                     new MqttZSort(ISort.Mode.LINK, ISort.Type.SERVER))),
    WS_QTT_SERVER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                     ISort.Type.SERVER,
                                     new WsProxyZSort<>(ISort.Mode.LINK,
                                                        ISort.Type.SERVER,
                                                        new MqttZSort(ISort.Mode.LINK, ISort.Type.SERVER))));

    private final IAioSort<?> _Sort;

    <C extends IPContext> ZSortHolder(IAioSort<C> sort)
    {
        _Sort = sort;
    }

    public static IControl CREATE(int serial, byte[] data)
    {
        if(serial >= X111_QttConnect.COMMAND && serial <= X11E_QttDisconnect.COMMAND) {
            return _QttFactory.create(serial, data, null);
        }
        if(serial >= 0x20 && serial <= 0x6F) {return _ServerFactory.create(serial, data, null);}
        if(serial >= X70_RaftVote.COMMAND && serial <= X77_RaftNotify.COMMAND) {
            return _ClusterFactory.create(serial, data, null);
        }
        throw new IllegalArgumentException();
    }

    final static ZServerFactory  _ServerFactory  = new ZServerFactory();
    final static ZClusterFactory _ClusterFactory = new ZClusterFactory();
    final static QttFactory      _QttFactory     = new QttFactory();

    @SuppressWarnings("unchecked")
    public <C extends IPContext> IAioSort<C> getSort()
    {
        return (IAioSort<C>) _Sort;
    }

    public int getSlot()
    {
        ISort.Mode mode = _Sort.getMode();
        ISort.Type type = _Sort.getType();
        return mode == ISort.Mode.LINK ? TYPE_CONSUMER_SLOT : type == ISort.Type.INNER ? TYPE_INTERNAL_SLOT
                                                                                       : type == ISort.Type.SERVER
                                                                                         ? TYPE_PROVIDER_SLOT
                                                                                         : TYPE_CLUSTER_SLOT;
    }

    public long getType()
    {
        ISort.Mode mode = _Sort.getMode();
        ISort.Type type = _Sort.getType();
        return mode == ISort.Mode.LINK ? TYPE_CONSUMER : type == ISort.Type.INNER ? ZUID.TYPE_INTERNAL
                                                                                  : type == ISort.Type.SERVER
                                                                                    ? TYPE_PROVIDER : TYPE_CLUSTER;
    }
}
