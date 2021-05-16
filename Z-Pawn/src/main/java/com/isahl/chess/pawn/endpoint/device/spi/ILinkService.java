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

import com.isahl.chess.bishop.io.IRouter;
import com.isahl.chess.bishop.io.mqtt.handler.IQttRouter;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceEntity;
import com.isahl.chess.queen.io.core.inf.IQoS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ILinkService
{
    void subscribe(Map<String,
                       IQoS.Level> subscribes,
                   long deviceId,
                   Function<Optional<DeviceEntity>,
                            BiConsumer<String,
                                       IQoS.Level>> function);

    void unsubscribe(List<String> topics,
                     long deviceId,
                     Function<Optional<DeviceEntity>,
                              Consumer<String>> function);

    void offline(long deviceId, IRouter router);

    DeviceEntity findDeviceByToken(String token);

    void clean(long origin, IRouter router);

    void load(long origin, IQttRouter router);

    void onLogin(long origin,
                 boolean hasWill,
                 String willTopic,
                 IQoS.Level willQoS,
                 boolean willRetain,
                 byte[] willMessage);
}
