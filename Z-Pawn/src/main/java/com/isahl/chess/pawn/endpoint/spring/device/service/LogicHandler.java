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

package com.isahl.chess.pawn.endpoint.spring.device.service;

import static com.isahl.chess.bishop.io.Direction.CLIENT_TO_SERVER;
import static com.isahl.chess.bishop.io.Direction.OWNER_CLIENT;
import static com.isahl.chess.bishop.io.Direction.OWNER_SERVER;
import static com.isahl.chess.bishop.io.Direction.SERVER_TO_CLIENT;
import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_INSERT;
import static com.isahl.chess.queen.db.inf.IStorage.Operation.OP_MODIFY;
import static com.isahl.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;
import static com.isahl.chess.queen.io.core.inf.IQoS.Level.EXACTLY_ONCE;
import static java.lang.Math.min;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.isahl.chess.bishop.io.mqtt.control.X113_QttPublish;
import com.isahl.chess.bishop.io.mqtt.control.X114_QttPuback;
import com.isahl.chess.bishop.io.mqtt.control.X115_QttPubrec;
import com.isahl.chess.bishop.io.mqtt.control.X116_QttPubrel;
import com.isahl.chess.bishop.io.mqtt.control.X117_QttPubcomp;
import com.isahl.chess.bishop.io.mqtt.control.X11C_QttPingreq;
import com.isahl.chess.bishop.io.mqtt.control.X11D_QttPingresp;
import com.isahl.chess.bishop.io.mqtt.handler.IQttRouter;
import com.isahl.chess.bishop.io.ws.control.X101_HandShake;
import com.isahl.chess.bishop.io.ws.control.X103_Ping;
import com.isahl.chess.bishop.io.ws.control.X104_Pong;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.Status;
import com.isahl.chess.knight.json.JsonUtil;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageBody;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageEntity;
import com.isahl.chess.pawn.endpoint.spring.device.jpa.repository.IMessageJpaRepository;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.inf.IActivity;
import com.isahl.chess.queen.io.core.inf.IClusterPeer;
import com.isahl.chess.queen.io.core.inf.IClusterTimer;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.INode;
import com.isahl.chess.queen.io.core.inf.IQoS;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 */
public class LogicHandler<T extends IActivity & IClusterPeer & IClusterTimer & INode & ISessionManager>
        implements
        ILogicHandler
{
    private final Logger                _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());
    private final T                     _Manager;
    private final IQttRouter            _QttRouter;
    private final RaftNode<T>           _RaftNode;
    private final IMessageJpaRepository _MessageRepository;

    public LogicHandler(T manager,
                        IQttRouter qttRouter,
                        RaftNode<T> raftNode,
                        IMessageJpaRepository messageRepository)
    {
        _Manager = manager;
        _QttRouter = qttRouter;
        _RaftNode = raftNode;
        _MessageRepository = messageRepository;
    }

    @Override
    public ISessionManager getISessionManager()
    {
        return _Manager;
    }

    @Override
    public IControl[] handle(ISessionManager manager, ISession session, IControl content) throws ZException
    {
        switch (content.serial())
        {
            case X101_HandShake.COMMAND:
                return new IControl[] { content
                };
            case X103_Ping.COMMAND:
                X103_Ping x103 = (X103_Ping) content;
                return new IControl[] { new X104_Pong(x103.getPayload())
                };
            case X113_QttPublish.COMMAND:
                X113_QttPublish x113 = (X113_QttPublish) content;
                MessageEntity messageEntity = new MessageEntity();
                messageEntity.setOrigin(session.getIndex());
                messageEntity.setDestination(_RaftNode.getPeerId());
                messageEntity.setDirection(CLIENT_TO_SERVER.getShort());
                messageEntity.setOwner(x113.getLevel()
                                           .getValue() < EXACTLY_ONCE.getValue() ? OWNER_SERVER
                                                                                 : OWNER_CLIENT);
                messageEntity.setBody(new MessageBody(x113.getTopic(), JsonUtil.readTree(x113.getPayload())));
                messageEntity.setMsgId(x113.getMsgId());
                messageEntity.setCmd(X113_QttPublish.COMMAND);
                messageEntity.setOperation(OP_INSERT);
                messageEntity.setStatus(Status.CREATED);
                _MessageRepository.save(messageEntity);
                List<IControl> pushList = new LinkedList<>();
                switch (x113.getLevel())
                {
                    case EXACTLY_ONCE:
                        X115_QttPubrec x115 = new X115_QttPubrec();
                        x115.setMsgId(x113.getMsgId());
                        _QttRouter.register(x115, session.getIndex());
                        // 此时尚未完成 message owner 的转换，所以只返回x115
                        return new IControl[] { x115
                        };
                    case AT_LEAST_ONCE:
                        X114_QttPuback x114 = new X114_QttPuback();
                        x114.setMsgId(x113.getMsgId());
                        pushList.add(x114);
                    default:
                        brokerTopic(manager, messageEntity, x113.getLevel(), pushList);
                        return pushList.toArray(new IControl[0]);
                }
            case X114_QttPuback.COMMAND:
                X114_QttPuback x114 = (X114_QttPuback) content;
                _QttRouter.ack(x114, session.getIndex());
                break;
            case X115_QttPubrec.COMMAND:
                X115_QttPubrec x115 = (X115_QttPubrec) content;
                _QttRouter.ack(x115, session.getIndex());
                MessageEntity update = _MessageRepository.findByOriginAndDestinationAndMsgIdAndCreatedAtAfter(_RaftNode.getPeerId(),
                                                                                                              session.getIndex(),
                                                                                                              x115.getMsgId(),
                                                                                                              LocalDateTime.now()
                                                                                                                           .minusSeconds(5));
                update.setOwner(OWNER_CLIENT);
                update.setOperation(OP_MODIFY);
                _MessageRepository.save(update);
                X116_QttPubrel x116 = new X116_QttPubrel();
                x116.setMsgId(x115.getMsgId());
                _QttRouter.register(x116, session.getIndex());
                return new IControl[] { x116
                };
            case X116_QttPubrel.COMMAND:
                x116 = (X116_QttPubrel) content;
                X117_QttPubcomp x117 = new X117_QttPubcomp();
                x117.setMsgId(x116.getMsgId());
                _QttRouter.ack(x116, session.getIndex());
                pushList = new LinkedList<>();
                pushList.add(x117);
                update = _MessageRepository.findByOriginAndDestinationAndMsgIdAndCreatedAtAfter(session.getIndex(),
                                                                                                _RaftNode.getPeerId(),
                                                                                                x116.getMsgId(),
                                                                                                LocalDateTime.now()
                                                                                                             .minusSeconds(5));
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
                return new IControl[] { new X11D_QttPingresp()
                };
        }
        return null;
    }

    private void brokerTopic(ISessionManager manager, MessageEntity message, IQoS.Level level, List<IControl> pushList)
    {
        Map<Long,
            IQoS.Level> route = _QttRouter.broker(message.getBody()
                                                         .getTopic());
        _Logger.debug("route %s", route);
        route.entrySet()
             .stream()
             .map(entry ->
             {
                 long sessionIndex = entry.getKey();
                 ISession targetSession = manager.findSessionByIndex(sessionIndex);
                 if (targetSession != null) {
                     IQoS.Level subscribeLevel = entry.getValue();
                     X113_QttPublish publish = new X113_QttPublish();
                     publish.setLevel(IQoS.Level.valueOf(min(subscribeLevel.getValue(), level.getValue())));
                     publish.setTopic(message.getBody()
                                             .getTopic());
                     byte[] payload = message.getBody()
                                             .contentBinary();
                     publish.setPayload(payload);
                     publish.setSession(targetSession);
                     if (publish.getLevel() == ALMOST_ONCE) { return publish; }
                     MessageEntity brokerMsg = new MessageEntity();
                     brokerMsg.setMsgId(_QttRouter.nextId());
                     brokerMsg.setOrigin(_RaftNode.getPeerId());
                     brokerMsg.setDestination(sessionIndex);
                     brokerMsg.setOperation(OP_INSERT);
                     brokerMsg.setOwner(OWNER_SERVER);
                     brokerMsg.setDirection(SERVER_TO_CLIENT.getShort());
                     brokerMsg.setBody(message.getBody());
                     brokerMsg.setCmd(X113_QttPublish.COMMAND);
                     _MessageRepository.save(brokerMsg);
                     publish.setMsgId(brokerMsg.getMsgId());
                     _QttRouter.register(publish, sessionIndex);
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
