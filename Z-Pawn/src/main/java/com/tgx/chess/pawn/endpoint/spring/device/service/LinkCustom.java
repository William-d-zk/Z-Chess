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

package com.tgx.chess.pawn.endpoint.spring.device.service;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.ZSort;
import com.tgx.chess.bishop.io.mqtt.QttContext;
import com.tgx.chess.bishop.io.mqtt.control.X111_QttConnect;
import com.tgx.chess.bishop.io.mqtt.control.X112_QttConnack;
import com.tgx.chess.bishop.io.mqtt.control.X118_QttSubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X119_QttSuback;
import com.tgx.chess.bishop.io.mqtt.control.X11A_QttUnsubscribe;
import com.tgx.chess.bishop.io.mqtt.control.X11B_QttUnsuback;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X108_Shutdown;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftResult;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.knight.raft.model.RaftCode;
import com.tgx.chess.pawn.endpoint.spring.device.model.DeviceEntry;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.mix.ILinkCustom;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.MixManager;

@Component
public class LinkCustom
        implements
        ILinkCustom<ZContext>
{
    private final Logger                                                                       _Logger    = Logger.getLogger(getClass().getSimpleName());
    private final IRepository<DeviceEntry>                                                     _DeviceRepository;
    private final NavigableMap<Long,
                               Triple<IControl<ZContext>,
                                      ISession<ZContext>,
                                      RaftCode>>                                               _NotifyMap = new TreeMap<>();
    private IQttRouter                                                                         mQttRouter;

    @Autowired
    public LinkCustom(IRepository<DeviceEntry> deviceRepository)
    {
        _DeviceRepository = deviceRepository;
    }

    @Override
    public IPair handle(MixManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> input) throws Exception
    {
        switch (input.serial())
        {
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) input;
                DeviceEntry device = new DeviceEntry();
                device.setToken(x111.getClientId());
                device.setUsername(x111.getUserName());
                device.setPassword(x111.getPassword());
                device.setOperation(IStorage.Operation.OP_APPEND);
                device = _DeviceRepository.find(device);
                X112_QttConnack x112 = new X112_QttConnack();
                x112.responseOk();
                /*--检查X112 是否正常--*/
                int[] supportVersions = QttContext.getSupportVersion()
                                                  .getSecond();
                if (Arrays.stream(supportVersions)
                          .noneMatch(version -> version == x111.getProtocolVersion()))
                {
                    x112.rejectUnacceptableProtocol();
                }
                else if (!x111.isClean() && x111.getClientIdLength() == 0) {
                    x112.rejectIdentifier();
                }
                /*--检查device 是否正确，验证账户密码--*/
                if (device == null) {
                    x112.rejectIdentifier();
                }
                else if (!device.getUsername()
                                .equalsIgnoreCase(x111.getUserName())
                         || !device.getPassword()
                                   .equals(x111.getPassword()))
                {
                    /*
                     * @see DeviceEntity
                     *      username >=8 && <=32
                     *      password >=5 && <=32
                     *      no_empty
                     */
                    x112.rejectNotAuthorized();
                }
                if (x112.isOk() && device != null) {
                    ISession<ZContext> old = manager.mapSession(device.getPrimaryKey(), session);
                    if (old != null) {
                        X108_Shutdown x108 = new X108_Shutdown();
                        x108.setSession(old);
                        return new Pair<>(new X108_Shutdown[] { x108 }, x111);
                    }
                    else {
                        return new Pair<>(null, x111);
                    }
                }
                else {
                    return new Pair<>(new X112_QttConnack[] { x112 }, null);
                }
            case X118_QttSubscribe.COMMAND:
            case X11A_QttUnsubscribe.COMMAND:
                return new Pair<>(null, input);
        }
        return null;
    }

    @Override
    public List<ITriple> notify(MixManager<ZContext> manager, IControl<ZContext> response, ISession<ZContext> session)
    {
        /*
        标准情况是 request.session, 但是在集群处理时x76携带了cluster 领域的session 作为入参，
        在transfer 之前并未按照接口要求进行转换，此处实现为一个优化项，使findSessionByIndex操作线程更为安全
         */
        IControl<ZContext> request;
        if (response.serial() == X76_RaftResult.COMMAND) {
            /*
            raft_client -> Link, session belong to cluster
            */
            X76_RaftResult x76 = (X76_RaftResult) response;
            int cmd = x76.getPayloadSerial();
            request = ZSort.getCommandFactory(cmd)
                           .create(cmd);
            request.decode(x76.getPayload());
            session = manager.findSessionByIndex(x76.getOrigin());
        }
        else {
            request = response;
        }
        Stream<IControl<ZContext>> handled = mappingHandle(manager, request, session);
        if (handled != null) {
            return handled.filter(Objects::nonNull)
                          .map(control -> new Triple<>(control,
                                                       control.getSession(),
                                                       control.getSession()
                                                              .getContext()
                                                              .getSort()
                                                              .getEncoder()))
                          .collect(Collectors.toList());
        }
        return null;
    }

    public void setQttRouter(IQttRouter qttRouter)
    {
        mQttRouter = qttRouter;
    }

    public DeviceEntry saveDevice(DeviceEntry entry)
    {
        return _DeviceRepository.save(entry);
    }

    public DeviceEntry findDevice(DeviceEntry entry)
    {
        return _DeviceRepository.find(entry);
    }

    private Stream<IControl<ZContext>> mappingHandle(MixManager manager,
                                                     IControl<ZContext> input,
                                                     ISession<ZContext> session)
    {

        switch (input.serial())
        {
            case X111_QttConnect.COMMAND:
                if (session != null) {
                    X112_QttConnack x112 = new X112_QttConnack();
                    x112.responseOk();
                    x112.setSession(session);
                    return Stream.of(x112);
                }
                break;
            case X118_QttSubscribe.COMMAND:
                X118_QttSubscribe x118 = (X118_QttSubscribe) input;
                List<Pair<String,
                          IQoS.Level>> topics = x118.getTopics();
                if (topics != null) {
                    X119_QttSuback x119 = new X119_QttSuback();
                    x119.setMsgId(x118.getMsgId());
                    topics.forEach(topic -> x119.addResult(mQttRouter.addTopic(topic,
                                                                               session.getIndex()) ? topic.getSecond()
                                                                                                   : IQoS.Level.FAILURE));
                    if (session != null) {
                        x119.setSession(session);
                        return Stream.of(x119);
                    }
                }
                break;
            case X11A_QttUnsubscribe.COMMAND:
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) input;
                x11A.getTopics()
                    .forEach(topic -> mQttRouter.removeTopic(topic, session.getIndex()));
                if (session != null) {
                    X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                    x11B.setMsgId(x11A.getMsgId());
                    x11B.setSession(session);
                    return Stream.of(x11B);
                }
                break;
        }
        return null;
    }
}
