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

package com.isahl.chess.bishop.sort;

import com.isahl.chess.bishop.sort.mqtt.MqttZSort;
import com.isahl.chess.bishop.sort.ssl.SslZSort;
import com.isahl.chess.bishop.sort.ws.WsTextZSort;
import com.isahl.chess.bishop.sort.ws.proxy.WsProxyZSort;
import com.isahl.chess.bishop.sort.zchat.ZSort;
import com.isahl.chess.bishop.sort.zchat.ZlsZSort;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.net.socket.features.IAioSort;

import static com.isahl.chess.king.env.ZUID.*;

/**
 * @author william.d.zk
 * @date 2019-06-30
 */
public enum ZSortHolder
{
    WS_PLAIN_TEXT_CONSUMER(new WsTextZSort(ISort.Mode.LINK, ISort.Type.CLIENT)),
    WS_PLAIN_TEXT_CONSUMER_SSL(new SslZSort<>(ISort.Mode.LINK,
                                              ISort.Type.CLIENT,
                                              new WsTextZSort(ISort.Mode.LINK, ISort.Type.CLIENT))),
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
                                                        new MqttZSort(ISort.Mode.LINK, ISort.Type.SERVER)))),
    Z_CLUSTER_SYMMETRY(new ZSort(ISort.Mode.CLUSTER, ISort.Type.SYMMETRY)),
    Z_CLUSTER_SYMMETRY_ZLS(new ZlsZSort(ISort.Mode.CLUSTER, ISort.Type.SYMMETRY));

    private final IAioSort<?> _Sort;

    <C extends IPContext> ZSortHolder(IAioSort<C> sort)
    {
        _Sort = sort;
    }

    @SuppressWarnings("unchecked")
    public <C extends IPContext> IAioSort<C> getSort()
    {
        return (IAioSort<C>) _Sort;
    }

    public int getSlot()
    {
        return getAttribute().getFirst();
    }

    private Pair<Integer, Long> getAttribute()
    {
        ISort.Mode mode = _Sort.getMode();
        ISort.Type type = _Sort.getType();
        return switch(mode) {
            case LINK -> Pair.of(TYPE_CLUSTER_SLOT, TYPE_CONSUMER);
            case CLUSTER -> switch(type) {
                case INNER -> Pair.of(TYPE_INTERNAL_SLOT, TYPE_INTERNAL);
                case SERVER -> Pair.of(TYPE_PROVIDER_SLOT, TYPE_PROVIDER);
                case SYMMETRY, CLIENT -> Pair.of(TYPE_CLUSTER_SLOT, TYPE_CLUSTER);
            };
        };
    }

}
