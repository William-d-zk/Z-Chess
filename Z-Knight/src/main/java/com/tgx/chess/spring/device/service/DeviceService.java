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

package com.tgx.chess.spring.device.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.tgx.chess.bishop.biz.device.DeviceNode;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X20_SignUp;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X21_SignUpResult;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X22_SignIn;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X23_SignInResult;
import com.tgx.chess.queen.io.external.websokcet.bean.device.X24_UpdateToken;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.repository.ClientRepository;
import com.tgx.chess.spring.device.repository.DeviceRepository;

@Service
@PropertySource("classpath:device.properties")
public class DeviceService
        implements
        IRepository
{
    private final DeviceRepository _DeviceRepository;
    private final ClientRepository _ClientRepository;
    private final DeviceNode       _DeviceNode;
    private final Random           _Random    = new Random();
    private final CryptUtil        _CryptUtil = new CryptUtil();

    @Autowired
    public DeviceService(DeviceRepository deviceRepository,
                         ClientRepository clientRepository,
                         @Value("${device.server.host}") String host,
                         @Value("${device.server.port}") int port)
    {
        _DeviceRepository = deviceRepository;
        _ClientRepository = clientRepository;
        _DeviceNode       = new DeviceNode(host, port, this);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _DeviceNode.start();
    }

    public List<DeviceEntity> findAll()
    {
        return _DeviceRepository.findAll();
    }

    public DeviceEntity saveDevice(DeviceEntity device)
    {
        return _DeviceRepository.save(device);
    }

    public DeviceEntity findDevice(String mac)
    {
        return _DeviceRepository.findByMac(mac);
    }

    @Override
    public ICommand save(ICommand tar)
    {
        switch (tar.getSerial())
        {
            case X20_SignUp.COMMAND:
                X20_SignUp x20 = (X20_SignUp) tar;
                byte[] deviceMac = x20.getMac();
                String devicePwd = x20.getPassword();
                long pwdId = x20.getPasswordId();
                DeviceEntity device = _DeviceRepository.findByMac(IoUtil.readMac(deviceMac));
                X21_SignUpResult x21 = new X21_SignUpResult();
                success:
                {
                    if (Objects.isNull(device) || device.getPasswordId() == pwdId) {
                        if (Objects.isNull(device)) {
                            device = new DeviceEntity();
                            device.setMac(IoUtil.readMac(deviceMac));
                            device.setPasswordId(pwdId);

                        }
                        device.setPassword(devicePwd);
                        device.setInvalidAt(Date.from(Instant.now()
                                                             .plusSeconds(TimeUnit.DAYS.toSeconds(41))));
                        byte[] src = new byte[6 + devicePwd.getBytes().length];
                        IoUtil.write(deviceMac, src, 0);
                        IoUtil.write(devicePwd.getBytes(), src, 6);
                        device.setToken(IoUtil.bin2Hex(_CryptUtil.sha256(src)));
                        try {
                            _DeviceRepository.save(device);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            x21.setFailed();
                            break success;
                        }
                        x21.setSuccess();
                        break success;
                    }
                    x21.setFailed();
                }
                if (x21.isSuccess()) {
                    x21.setToken(IoUtil.hex2bin(device.getToken()));
                }
                x21.setPasswordId(pwdId);
                return x21;
            case X24_UpdateToken.COMMAND:
                X24_UpdateToken x24 = (X24_UpdateToken) tar;
                break;

        }
        return null;
    }

    @Override
    public ICommand find(ICommand key)
    {
        switch (key.getSerial())
        {
            case X22_SignIn.COMMAND:
                X22_SignIn x22 = (X22_SignIn) key;
                X23_SignInResult x23 = new X23_SignInResult();
                String deviceToken = IoUtil.bin2Hex(x22.getToken());
                String devicePwd = x22.getPassword();
                login:
                {
                    byte[] password = devicePwd.getBytes();
                    byte[] toSign   = new byte[password.length + 6];
                    IoUtil.write(password, toSign, 6);
                    DeviceEntity device = _DeviceRepository.findByTokenAndPassword(deviceToken, devicePwd);
                    if (Objects.nonNull(device)) {
                        byte[] macRaw = IoUtil.writeMacRaw(device.getMac());
                        IoUtil.write(macRaw, toSign, 0);
                        String sign = IoUtil.bin2Hex(_CryptUtil.sha256(toSign));
                        if (sign.equals(deviceToken)) {
                            x23.setSuccess();
                            Date date = device.getInvalidAt();
                            x23.setInvalidTime(date.getTime());
                            break login;
                        }
                    }
                    x23.setFailed();
                }
                return x23;
        }
        return null;
    }
}
