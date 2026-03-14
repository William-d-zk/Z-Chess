/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.audience.client.stress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.king.base.log.Logger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 设备凭证加载器
 * 
 * 从JSON文件加载MQTT测试设备凭证，支持：
 * - 批量加载设备凭证
 * - 随机获取设备用于压测
 * - 循环使用设备池
 */
@Component
public class DeviceCredentialLoader
{
    private static final Logger _Logger = Logger.getLogger("stress.device");

    @Value("${device.credentials.file:classpath:test-devices.json}")
    private Resource deviceFile;

    @Value("${device.credentials.path:}")
    private String deviceFilePath;

    private final List<DeviceCredential> devices = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init()
    {
        loadDevices();
    }

    /**
     * 加载设备凭证
     */
    public void loadDevices()
    {
        try {
            JsonNode root;
            if(deviceFilePath != null && !deviceFilePath.isEmpty()) {
                root = objectMapper.readTree(new java.io.File(deviceFilePath));
            }
            else if(deviceFile != null && deviceFile.exists()) {
                root = objectMapper.readTree(deviceFile.getInputStream());
            }
            else {
                _Logger.warning("No device credentials file found, using default path: /app/data/devices-100.json");
                // Try default path in Docker container
                java.io.File defaultFile = new java.io.File("/app/data/devices-100.json");
                if(defaultFile.exists()) {
                    root = objectMapper.readTree(defaultFile);
                }
                else {
                    _Logger.warning("Default device file not found, devices list is empty");
                    return;
                }
            }

            JsonNode devicesNode = root.get("devices");
            if(devicesNode == null || !devicesNode.isArray()) {
                _Logger.warning("Invalid device file format, expected 'devices' array");
                return;
            }

            devices.clear();
            for(JsonNode deviceNode : devicesNode) {
                DeviceCredential credential = new DeviceCredential();
                credential.username = getTextOrDefault(deviceNode, "username", "");
                credential.token = getTextOrDefault(deviceNode, "token", "");
                credential.serial = getTextOrDefault(deviceNode, "serial", "");
                credential.publicKey = getTextOrDefault(deviceNode, "public_key", "");
                credential.mqttUsername = getTextOrDefault(deviceNode, "mqtt_username", credential.token);
                credential.mqttPassword = getTextOrDefault(deviceNode, "mqtt_password", credential.token);
                credential.mqttClientId = getTextOrDefault(deviceNode, "mqtt_client_id", credential.serial);
                credential.expireDate = getTextOrDefault(deviceNode, "expire_date", "");
                
                if(!credential.token.isEmpty() && !credential.serial.isEmpty()) {
                    devices.add(credential);
                }
            }

            _Logger.info("Loaded %d device credentials", devices.size());
        }
        catch(IOException e) {
            _Logger.warning("Failed to load device credentials: %s", e.getMessage());
        }
    }

    /**
     * 获取所有设备
     */
    public List<DeviceCredential> getAllDevices()
    {
        return Collections.unmodifiableList(devices);
    }

    /**
     * 获取指定数量的设备（循环使用）
     */
    public List<DeviceCredential> getDevices(int count)
    {
        if(devices.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeviceCredential> result = new ArrayList<>(count);
        for(int i = 0; i < count; i++) {
            result.add(devices.get(i % devices.size()));
        }
        return result;
    }

    /**
     * 随机获取一个设备
     */
    public DeviceCredential getRandomDevice()
    {
        if(devices.isEmpty()) {
            return null;
        }
        return devices.get(ThreadLocalRandom.current().nextInt(devices.size()));
    }

    /**
     * 随机获取指定数量的设备
     */
    public List<DeviceCredential> getRandomDevices(int count)
    {
        if(devices.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeviceCredential> result = new ArrayList<>(count);
        for(int i = 0; i < count; i++) {
            result.add(getRandomDevice());
        }
        return result;
    }

    /**
     * 获取设备数量
     */
    public int getDeviceCount()
    {
        return devices.size();
    }

    /**
     * 是否有可用设备
     */
    public boolean hasDevices()
    {
        return !devices.isEmpty();
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue)
    {
        JsonNode valueNode = node.get(field);
        return valueNode != null ? valueNode.asText(defaultValue) : defaultValue;
    }

    /**
     * 设备凭证
     */
    public static class DeviceCredential
    {
        public String username;
        public String token;
        public String serial;
        public String publicKey;
        public String mqttUsername;
        public String mqttPassword;
        public String mqttClientId;
        public String expireDate;

        @Override
        public String toString()
        {
            return String.format("DeviceCredential{username=%s, serial=%s...}", 
                username, 
                serial != null && serial.length() > 8 ? serial.substring(0, 8) + "..." : serial);
        }
    }
}
