/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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

package com.tgx.chess.spring.device.service;

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.tgx.chess.spring.jpa.device.dao.DeviceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.control.*;
import com.tgx.chess.bishop.io.mqtt.handler.IQttRouter;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.device.X20_SignUp;
import com.tgx.chess.bishop.io.zprotocol.device.X21_SignUpResult;
import com.tgx.chess.bishop.io.zprotocol.device.X24_UpdateToken;
import com.tgx.chess.bishop.io.zprotocol.device.X25_AuthorisedToken;
import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.inf.ICustomLogic;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IQoS;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;
import com.tgx.chess.spring.device.model.DeviceEntry;

@Component
public class LinkCustom
        implements
        ICustomLogic<ZContext>
{
    private final IRepository<DeviceEntry> _DeviceRepository;
    private IQttRouter                     mQttRouter;

    @Autowired
    public LinkCustom(IRepository<DeviceEntry> deviceRepository)
    {
        _DeviceRepository = deviceRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<ZContext>[] handle(QueenManager<ZContext> manager,
                                       ISession<ZContext> session,
                                       IControl<ZContext> content) throws ZException
    {
        switch (content.serial())
        {
            case X20_SignUp.COMMAND:
                //TODO 重新设计X20
                X20_SignUp x20 = (X20_SignUp) content;
                X21_SignUpResult x21 = new X21_SignUpResult();
                return new IControl[] { x21 };
            case X24_UpdateToken.COMMAND:
                //TODO 重新设计X24
                X24_UpdateToken x24 = (X24_UpdateToken) content;
                X25_AuthorisedToken x25 = new X25_AuthorisedToken();
                return new IControl[] { x25 };
            case X111_QttConnect.COMMAND:
                X111_QttConnect x111 = (X111_QttConnect) content;
                DeviceEntry device = new DeviceEntry();
                device.setToken(x111.getClientId());
                device.setUsername(x111.getUserName());
                device.setPassword(x111.getPassword());
                device = _DeviceRepository.find(device);
                X112_QttConnack x112 = new X112_QttConnack();
                x112.responseOk();
                /*--检查X112 是否正常--*/
                int[] supportVersions = QttContext.getSupportVersion()
                                                  .second();
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
                    /**
                     * @see DeviceEntity
                     *      username >=8 && <=32
                     *      password >=5 && <=32
                     *      no_empty
                     */
                    x112.rejectNotAuthorized();
                }
                if (!x112.isIllegalState()) {
                    manager.mapSession(device.getPrimaryKey(), session);
                }
                return new IControl[] { x112 };
            case X118_QttSubscribe.COMMAND:
                X118_QttSubscribe x118 = (X118_QttSubscribe) content;
                X119_QttSuback x119 = new X119_QttSuback();
                x119.setMsgId(x118.getMsgId());
                List<Pair<String,
                          IQoS.Level>> topics = x118.getTopics();
                if (topics != null) {
                    topics.forEach(topic -> x119.addResult(mQttRouter.addTopic(topic,
                                                                               session.getIndex()) ? topic.second()
                                                                                                   : IQoS.Level.FAILURE));
                }
                return new IControl[] { x119 };
            case X11A_QttUnsubscribe.COMMAND:
                X11A_QttUnsubscribe x11A = (X11A_QttUnsubscribe) content;
                x11A.getTopics()
                    .forEach(topic -> mQttRouter.removeTopic(topic, session.getIndex()));
                X11B_QttUnsuback x11B = new X11B_QttUnsuback();
                x11B.setMsgId(x11A.getMsgId());
                return new IControl[] { x11B };
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
}
