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

package com.tgx.chess.device.api;

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.spring.auth.model.AccountEntity;
import com.tgx.chess.spring.auth.service.AccountService;
import com.tgx.chess.spring.auth.service.ClientService;
import com.tgx.chess.spring.device.model.ClientEntity;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.service.ClientDo;
import com.tgx.chess.spring.device.service.DeviceService;

/**
 * @author william.d.zk
 * @date 2019-07-19
 */
@RestController
public class ClientController
{

    private final Logger         _Logger = Logger.getLogger(getClass().getName());
    private final ClientService  _ClientService;
    private final AccountService _AccountService;
    private final DeviceService  _DeviceService;

    @Autowired
    public ClientController(ClientService clientService,
                            AccountService accountService,
                            DeviceService deviceService)
    {
        _ClientService = clientService;
        _AccountService = accountService;
        _DeviceService = deviceService;
    }

    @PostMapping("/client/register")
    public @ResponseBody Object register(@RequestBody ClientDo client)
    {
        AccountEntity accountEntity = _AccountService.authAccount(client.getAuth(),
                                                                  client.getCipher(),
                                                                  client.getUserName());
        if (accountEntity != null) {
            Set<ClientEntity> clients = accountEntity.getClients();
            ClientEntity clientEntity = null;
            if (clients != null) {
                for (ClientEntity entity : clients) {
                    if (!client.getUserName()
                               .equals(entity.getUserName())
                        && !isBlank(entity.getUserName()))
                    {
                        continue;
                    }
                    clientEntity = entity;
                    _Logger.info("client exist %s", clientEntity.getId());
                    client.setExist(true);
                    break;
                }
            }
            else {
                clients = new HashSet<>();
            }
            if (clientEntity == null) {
                clientEntity = new ClientEntity();
            }
            clients.add(clientEntity);
            accountEntity.setClients(clients);
            clientEntity.setUserName(client.getUserName());
            clientEntity.setAccount(accountEntity);
            List<String> devices = client.getDevices();
            if (devices == null || devices.isEmpty()) {
                throw new IllegalArgumentException("register empty client with none devices!");
            }
            Set<DeviceEntity> deviceSet = client.getDevices()
                                                .stream()
                                                .map(_DeviceService::findDeviceBySn)
                                                .filter(Objects::nonNull)
                                                .peek(device -> device.setUserName(client.getUserName()))
                                                .collect(Collectors.toSet());
            _DeviceService.updateDevices(deviceSet);
            Set<String> snSet = deviceSet.stream()
                                         .map(DeviceEntity::getSn)
                                         .collect(Collectors.toSet());
            Set<DeviceEntity> existDeviceSet = clientEntity.getDevices();
            if (existDeviceSet != null) {
                deviceSet.addAll(existDeviceSet);
            }
            clientEntity.setDevices(deviceSet);
            _ClientService.updateClient(clientEntity);
            _AccountService.updateAccount(accountEntity);
            client.setBindDevices(client.getDevices()
                                        .stream()
                                        .filter(snSet::contains)
                                        .collect(Collectors.toList()));
            client.getDevices()
                  .removeAll(snSet);
            client.setAuth(null);
            client.setCipher(null);
            return client;
        }
        return "authority failed";
    }

    @GetMapping("/client/devices")
    public @ResponseBody Object getDevices(@RequestParam String auth,
                                           @RequestParam String cipher,
                                           @RequestParam String userName)
    {
        AccountEntity accountEntity = _AccountService.authAccount(auth, cipher, userName);
        if (accountEntity != null) {
            Set<ClientEntity> clients = accountEntity.getClients();
            if (clients != null && !clients.isEmpty()) {
                return clients.stream()
                              .filter(clientEntity -> clientEntity.getUserName()
                                                                  .equals(userName))
                              .flatMap(clientEntity -> clientEntity.getDevices()
                                                                   .stream())
                              .map(_DeviceService::convertDevice)
                              .collect(Collectors.toList());
            }
        }
        return "no devices";
    }
}
