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

package com.tgx.chess.bishop.biz.device;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Objects;

import com.tgx.chess.bishop.biz.db.dao.DeviceEntry;
import com.tgx.chess.bishop.io.QttZSort;
import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.mqtt.handler.QttLinkHandler;
import com.tgx.chess.bishop.io.mqtt.handler.QttLogicHandler;
import com.tgx.chess.bishop.io.zcrypt.EncryptHandler;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 * @date 2019-05-12
 */
public class QttNode
        extends
        BaseDeviceNode<QttContext>
{

    public QttNode(String host,
                   int port,
                   IRepository<DeviceEntry,
                               QttContext> respository)
    {
        super(host, port, QttZSort.SYMMETRY, respository);
    }

    @Override
    public QttContext createContext(ISessionOption option, ISort sort)
    {
        return new QttContext(option);
    }

    public void start() throws IOException
    {
        _ServerCore.build(new QttLogicHandler(_Sort.getEncoder(), (command, session, handler) ->
        {
            //前置的 dispatcher 将 ICommands 拆分了

            _Logger.info("qtt node logic handle %s", command);
            switch (command.getSerial())
            {

                default:
                    break;
            }
            return null;
        }), this, _Sort.getEncoder(), new QttLinkHandler<>(), new EncryptHandler());
        _AioServer.bindAddress(new InetSocketAddress(_ServerHost, _ServerPort),
                               AsynchronousChannelGroup.withFixedThreadPool(_ServerCore.getServerCount(),
                                                                            _ServerCore.getWorkerThreadFactory()));
        _AioServer.pendingAccept();
        _Logger.info(String.format("qtt node start %s:%d", _ServerHost, _ServerPort));
    }

    @Override
    public IControl<QttContext> save(IControl<QttContext> tar, ISession<QttContext> session)
    {
        DeviceEntry deviceEntry = _Repository.save(tar);
        switch (tar.getSerial())
        {
            case X111_QttConnect.COMMAND:
                X112_QttConnack x112 = new X112_QttConnack();
                x112.responseOk();
                if (Objects.nonNull(deviceEntry)) {

                }
                return x112;
        }
        return null;
    }

    @Override
    public IControl<QttContext> find(IControl<QttContext> key, ISession<QttContext> session)
    {
        return null;
    }
}
