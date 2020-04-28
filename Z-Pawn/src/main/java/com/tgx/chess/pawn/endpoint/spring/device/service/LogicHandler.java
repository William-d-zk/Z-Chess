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

import static com.tgx.chess.bishop.io.Direction.CLIENT_TO_SERVER;
import static com.tgx.chess.bishop.io.Direction.OWNER_CLIENT;
import static com.tgx.chess.bishop.io.Direction.OWNER_SERVER;
import static com.tgx.chess.bishop.io.Direction.SERVER_TO_CLIENT;
import static com.tgx.chess.queen.db.inf.IStorage.Operation.OP_INSERT;
import static com.tgx.chess.queen.db.inf.IStorage.Operation.OP_MODIFY;
import static com.tgx.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;
import static com.tgx.chess.queen.io.core.inf.IQoS.Level.EXACTLY_ONCE;
import static java.lang.Math.min;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tgx.chess.bishop.io.mqtt.control.X113_QttPublish;
import com.tgx.chess.bishop.io.mqtt.control.X114_QttPuback;
import com.tgx.chess.bishop.io.mqtt.control.X115_QttPubrec;
import com.tgx.chess.bishop.io.mqtt.control.X116_QttPubrel;
import com.tgx.chess.bishop.io.mqtt.control.X117_QttPubcomp;
import com.tgx.chess.bishop.io.mqtt.control.X11C_QttPingreq;
import com.tgx.chess.bishop.io.mqtt.control.X11D_QttPingresp;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.pawn.endpoint.spring.device.model.MessageEntry;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.handler.mix.ILogicHandler;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;
import com.tgx.chess.queen.io.core.manager.MixManager;

/**
 * @author william.d.zk
 */
public class LogicHandler
        implements
        ILogicHandler<ZContext>
{
    private final Logger                    _Logger = Logger.getLogger(getClass().getSimpleName());
    private final ISessionManager<ZContext> _Manager;
    private final IQttRouter                _QttRouter;
    private final IRepository<MessageEntry> _MessageRepository;

    public LogicHandler(MixManager<ZContext> manager,
                        IQttRouter qttRouter,
                        IRepository<MessageEntry> messageRepository)
    {
        _Manager = manager;
        _QttRouter = qttRouter;
        _MessageRepository = messageRepository;
    }

    @Override
    public ISessionManager<ZContext> getISessionManager()
    {
        return _Manager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<ZContext>[] handle(ISessionManager<ZContext> manager,
                                       ISession<ZContext> session,
                                       IControl<ZContext> content) throws ZException
    {
        switch (content.serial())
        {
            case X113_QttPublish.COMMAND:
                X113_QttPublish x113 = (X113_QttPublish) content;
                MessageEntry messageEntry = new MessageEntry();
                messageEntry.setCmd(X113_QttPublish.COMMAND);
                messageEntry.setTopic(x113.getTopic());
                messageEntry.setOrigin(session.getIndex());
                messageEntry.setDestination(_MessageRepository.getPeerId());
                messageEntry.setDirection(CLIENT_TO_SERVER.getShort());
                messageEntry.setPayload(x113.getPayload());
                messageEntry.setOwner(x113.getLevel()
                                          .getValue() < EXACTLY_ONCE.getValue() ? OWNER_SERVER
                                                                                : OWNER_CLIENT);
                messageEntry.setMsgId(x113.getMsgId());
                messageEntry.setOperation(OP_INSERT);
                _MessageRepository.save(messageEntry);
                List<IControl<ZContext>> pushList = new LinkedList<>();
                switch (x113.getLevel())
                {
                    case EXACTLY_ONCE:
                        X115_QttPubrec x115 = new X115_QttPubrec();
                        x115.setMsgId(x113.getMsgId());
                        _QttRouter.register(x115, session.getIndex());
                        //此时尚未完成 message owner 的转换，所以只返回x115
                        return new IControl[] { x115 };
                    case AT_LEAST_ONCE:
                        X114_QttPuback x114 = new X114_QttPuback();
                        x114.setMsgId(x113.getMsgId());
                        pushList.add(x114);
                    default:
                        brokerTopic(manager, messageEntry, x113.getLevel(), pushList);
                        return pushList.toArray(new IControl[0]);
                }
            case X114_QttPuback.COMMAND:
                X114_QttPuback x114 = (X114_QttPuback) content;
                _QttRouter.ack(x114, session.getIndex());
                break;
            case X115_QttPubrec.COMMAND:
                X115_QttPubrec x115 = (X115_QttPubrec) content;
                _QttRouter.ack(x115, session.getIndex());
                MessageEntry update = new MessageEntry();
                update.setOrigin(_MessageRepository.getPeerId());
                update.setDestination(session.getIndex());
                update.setMsgId(x115.getMsgId());
                update.setOwner(OWNER_CLIENT);
                update.setOperation(OP_MODIFY);
                _MessageRepository.save(update);
                X116_QttPubrel x116 = new X116_QttPubrel();
                x116.setMsgId(x115.getMsgId());
                _QttRouter.register(x116, session.getIndex());
                return new IControl[] { x116 };
            case X116_QttPubrel.COMMAND:
                x116 = (X116_QttPubrel) content;
                X117_QttPubcomp x117 = new X117_QttPubcomp();
                x117.setMsgId(x116.getMsgId());
                _QttRouter.ack(x116, session.getIndex());
                pushList = new LinkedList<>();
                pushList.add(x117);
                update = new MessageEntry();
                update.setOrigin(session.getIndex());
                update.setDestination(_MessageRepository.getPeerId());
                update.setMsgId(x116.getMsgId());
                update.setOwner(OWNER_SERVER);
                update.setOperation(OP_MODIFY);
                update = _MessageRepository.save(update);
                brokerTopic(manager, update, EXACTLY_ONCE, pushList);
                return pushList.toArray(new IControl[0]);
            case X117_QttPubcomp.COMMAND:
                x117 = (X117_QttPubcomp) content;
                _QttRouter.ack(x117, session.getIndex());
                break;
            case X11C_QttPingreq.COMMAND:
                return new IControl[] { new X11D_QttPingresp() };
        }

        return new IControl[0];
    }

    private void brokerTopic(ISessionManager<ZContext> manager,
                             MessageEntry message,
                             IQoS.Level level,
                             List<IControl<ZContext>> pushList)
    {
        Map<Long,
            IQoS.Level> route = _QttRouter.broker(message.getTopic());
        _Logger.debug("route %s", route);
        route.entrySet()
             .stream()
             .map(entry ->
             {
                 ISession<ZContext> targetSession = manager.findSessionByIndex(entry.getKey());
                 if (targetSession != null) {
                     IQoS.Level subscribeLevel = entry.getValue();
                     X113_QttPublish publish = new X113_QttPublish();
                     publish.setLevel(IQoS.Level.valueOf(min(subscribeLevel.getValue(), level.getValue())));
                     publish.setTopic(message.getTopic());
                     publish.setPayload(message.getPayload());
                     publish.setSession(targetSession);
                     if (publish.getLevel() == ALMOST_ONCE) { return publish; }
                     long packIdentity = _QttRouter.nextPackIdentity();
                     publish.setMsgId(packIdentity);
                     message.setMsgId(packIdentity);
                     message.setOrigin(_MessageRepository.getPeerId());
                     message.setDestination(targetSession.getIndex());
                     message.setOperation(OP_INSERT);
                     message.setOwner(OWNER_SERVER);
                     message.setDirection(SERVER_TO_CLIENT.getShort());
                     _MessageRepository.save(message);
                     _QttRouter.register(publish, entry.getKey());
                     return publish;
                 }
                 return null;
             })
             .filter(Objects::nonNull)
             .forEach(pushList::add);
        _Logger.debug("push %s", pushList);
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
