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

package com.isahl.chess.pawn.endpoint.device.test;

import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.CryptoUtil.ASymmetricKeyPair;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.model.DeviceProfile;
import com.isahl.chess.pawn.endpoint.device.resource.model.DeviceProfile.KeyPairProfile;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * 测试设备生成器
 * 用于生成测试设备和登录信息
 */
public class DeviceGeneratorTest {

    private static final Random RANDOM = new Random();

    /**
     * 生成随机MAC地址
     */
    public static String generateMacAddress() {
        byte[] mac = new byte[6];
        RANDOM.nextBytes(mac);
        // 设置本地管理地址位
        mac[0] = (byte) (mac[0] | 0x02);
        mac[0] = (byte) (mac[0] & 0xfe);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02x", mac[i]));
        }
        return sb.toString();
    }

    /**
     * 生成测试设备实体
     */
    public static DeviceEntity generateTestDevice() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // 生成设备基本信息
        String deviceName = "test_device_" + uniqueId;
        String username = "testuser_" + uniqueId;
        String password = "TestPass_" + UUID.randomUUID().toString().substring(0, 8);
        String serialNumber = "TEST" + timestamp.substring(timestamp.length() - 10);
        
        // 生成MAC地址
        String ethernetMac = generateMacAddress();
        String wifiMac = generateMacAddress();
        String bluetoothMac = generateMacAddress();
        
        // 生成ECC密钥对
        ASymmetricKeyPair keyPair = CryptoUtil.generateEccKeyPair(256);
        
        // 创建设备配置
        DeviceProfile profile = new DeviceProfile();
        profile.setEthernetMac(ethernetMac);
        profile.setWifiMac(wifiMac);
        profile.setBluetoothMac(bluetoothMac);
        profile.setKeyPairProfile(new KeyPairProfile(
            keyPair.getAlgorithm(),
            keyPair.getPublicKey(),
            keyPair.getPrivateKey()
        ));
        
        // 创建设备实体
        DeviceEntity device = new DeviceEntity();
        device.setVNotice(serialNumber);
        device.setUsername(username);
        device.setPassword(password);
        device.setCode(deviceName);
        device.setProfile(profile);
        device.setCreatedById(1L);
        device.setUpdatedById(1L);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        return device;
    }

    /**
     * 生成仓娲项目格式的测试设备
     */
    public static DeviceEntity generateCangwaTestDevice() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // 生成MAC地址
        String ethernetMac = generateMacAddress();
        String wifiMac = generateMacAddress();
        String bluetoothMac = generateMacAddress();
        
        // 生成序列号 (MD5 of MACs)
        String serialNo = CryptoUtil.MD5(ethernetMac + "|" + wifiMac + "|" + bluetoothMac);
        
        // 生成ECC密钥对
        ASymmetricKeyPair keyPair = CryptoUtil.generateEccKeyPair(256);
        
        // 创建设备配置
        DeviceProfile profile = new DeviceProfile();
        profile.setEthernetMac(ethernetMac);
        profile.setWifiMac(wifiMac);
        profile.setBluetoothMac(bluetoothMac);
        profile.setKeyPairProfile(new KeyPairProfile(
            keyPair.getAlgorithm(),
            keyPair.getPublicKey(),
            keyPair.getPrivateKey()
        ));
        
        // 创建设备实体
        DeviceEntity device = new DeviceEntity();
        device.setVNotice(serialNo);
        device.setUsername("testuser_" + uniqueId);
        device.setCode("test_device_" + uniqueId);
        device.setProfile(profile);
        device.setCreatedById(1L);
        device.setUpdatedById(1L);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        return device;
    }

    /**
     * 打印设备登录信息
     */
    public static void printDeviceLoginInfo(DeviceEntity device) {
        System.out.println("========================================");
        System.out.println("【测试设备登录信息】");
        System.out.println("========================================");
        System.out.println("设备Token:     " + device.getToken());
        System.out.println("设备名称:      " + device.getCode());
        System.out.println("用户名:        " + device.getUsername());
        System.out.println("密码:          " + device.getPassword());
        System.out.println("设备编号:      " + device.getNotice());
        System.out.println("序列号:        " + device.getVNotice());
        System.out.println("----------------------------------------");
        System.out.println("MAC地址信息:");
        if (device.getProfile() != null) {
            System.out.println("  以太网MAC:   " + device.getProfile().getEthernetMac());
            System.out.println("  WiFi MAC:    " + device.getProfile().getWifiMac());
            System.out.println("  蓝牙MAC:     " + device.getProfile().getBluetoothMac());
        }
        System.out.println("----------------------------------------");
        System.out.println("密钥信息:");
        if (device.getProfile() != null && device.getProfile().getKeyPairProfile() != null) {
            System.out.println("  算法:        " + device.getProfile().getKeyPairProfile().getKeyPairAlgorithm());
            System.out.println("  公钥:        " + device.getProfile().getKeyPairProfile().getPublicKey().substring(0, 50) + "...");
        }
        System.out.println("========================================");
    }

    /**
     * 打印JSON格式的注册请求体
     */
    public static void printRegisterRequestJson(DeviceEntity device) {
        System.out.println("\n【/device/register 请求JSON】");
        System.out.println("----------------------------------------");
        System.out.println("{");
        System.out.println("  \"name\": \"" + device.getCode() + "\",");
        System.out.println("  \"username\": \"" + device.getUsername() + "\",");
        System.out.println("  \"number\": \"" + device.getNotice() + "\",");
        System.out.println("  \"profile\": {");
        if (device.getProfile() != null) {
            System.out.println("    \"ethernet_mac\": \"" + device.getProfile().getEthernetMac() + "\",");
            System.out.println("    \"wifi_mac\": \"" + device.getProfile().getWifiMac() + "\",");
            System.out.println("    \"bluetooth_mac\": \"" + device.getProfile().getBluetoothMac() + "\"");
        }
        System.out.println("  }");
        System.out.println("}");
        System.out.println("----------------------------------------");
    }

    /**
     * 打印JSON格式的init请求体
     */
    public static void printInitRequestJson(DeviceEntity device) {
        System.out.println("\n【/device/init 请求JSON】");
        System.out.println("----------------------------------------");
        System.out.println("{");
        System.out.println("  \"username\": \"" + device.getUsername() + "\",");
        System.out.println("  \"profile\": {");
        if (device.getProfile() != null) {
            System.out.println("    \"ethernet_mac\": \"" + device.getProfile().getEthernetMac() + "\",");
            System.out.println("    \"wifi_mac\": \"" + device.getProfile().getWifiMac() + "\",");
            System.out.println("    \"bluetooth_mac\": \"" + device.getProfile().getBluetoothMac() + "\"");
        }
        System.out.println("  }");
        System.out.println("}");
        System.out.println("----------------------------------------");
    }

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Z-Chess 测试设备生成器                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 生成普通测试设备
        System.out.println("【方式1】普通设备注册 /device/register");
        System.out.println("────────────────────────────────────────────────────────────");
        DeviceEntity testDevice = generateTestDevice();
        printDeviceLoginInfo(testDevice);
        printRegisterRequestJson(testDevice);

        System.out.println("\n");
        System.out.println("【方式2】仓娲项目初始化 /device/init");
        System.out.println("────────────────────────────────────────────────────────────");
        // 生成仓娲格式测试设备
        DeviceEntity cangwaDevice = generateCangwaTestDevice();
        printDeviceLoginInfo(cangwaDevice);
        printInitRequestJson(cangwaDevice);

        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  提示: 使用以上JSON请求体调用接口完成设备注册              ║");
        System.out.println("║  curl示例:                                                 ║");
        System.out.println("║  curl -X POST http://localhost:8080/device/register \\     ║");
        System.out.println("║       -H \"Content-Type: application/json\" \\              ║");
        System.out.println("║       -d '{...上面生成的JSON...}'                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}
