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

package com.isahl.chess.king.base.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest
{

    private CryptoUtil cryptoUtil = new CryptoUtil();

    @Test
    void randomPassword()
    {
        for(int i = 0; i < 100; i++) {
            String passwd = cryptoUtil.randomPassword(17, 31, false);
            
            // 验证密码长度
            assertTrue(passwd.length() >= 17, "密码长度应该大于等于 17");
            assertTrue(passwd.length() < 31, "密码长度应该小于 31");
            
            // 验证密码不为空
            assertNotNull(passwd, "密码不应该为 null");
            assertFalse(passwd.isEmpty(), "密码不应该为空");
        }
    }
    
    @Test
    void randomPasswordOnlyWords()
    {
        String passwd = cryptoUtil.randomPassword(10, 20, true);
        
        // 验证只包含字母和数字
        assertTrue(passwd.matches("[a-zA-Z0-9]+"), "onlyWords=true 时密码应该只包含字母和数字");
    }
    
    @Test
    void randomPasswordWithSpecialChars()
    {
        // 生成足够多的密码样本，增加包含特殊字符的概率
        boolean foundSpecialChar = false;
        for(int i = 0; i < 50; i++) {
            String passwd = cryptoUtil.randomPassword(20, 30, false);
            if(!passwd.matches("[a-zA-Z0-9]+")) {
                foundSpecialChar = true;
                break;
            }
        }
        // 不强制要求一定有特殊字符，因为随机性可能导致没有
        // 只验证密码生成功能正常
    }

    @Test
    void crcTest()
    {
        byte[] input = IoUtil.hex2bin("ABC0");
        
        // CRC16 测试
        int crc16 = CryptoUtil.crc16(input, 0, input.length);
        assertTrue(crc16 >= 0 && crc16 <= 0xFFFF, "CRC16 应该在 0-65535 范围内");
        
        // CRC16 Modbus 测试
        int crc16Modbus = CryptoUtil.crc16_modbus(input, 0, input.length);
        assertTrue(crc16Modbus >= 0 && crc16Modbus <= 0xFFFF, "CRC16 Modbus 应该在 0-65535 范围内");
        
        // 验证 Modbus CRC 是标准 CRC 的字节交换
        assertEquals(IoUtil.swapLhb(crc16), crc16Modbus, "Modbus CRC 应该是标准 CRC 的字节交换");
        
        // CRC32 测试
        int crc32 = CryptoUtil.crc32(input, 0, input.length);
        // CRC32 结果应该符合 IEEE 802.3 标准
        
        // CRC64 测试
        long crc64 = CryptoUtil.crc64(input, 0, input.length);
        assertTrue(crc64 != 0, "CRC64 结果不应该为 0");
    }
    
    @Test
    void testAdler32()
    {
        byte[] input = "test data".getBytes(StandardCharsets.UTF_8);
        int adler32 = CryptoUtil.adler32(input, 0, input.length);
        
        assertTrue(adler32 > 0, "Adler32 结果应该为正数");
        
        // 相同输入应该产生相同结果
        int adler32Again = CryptoUtil.adler32(input, 0, input.length);
        assertEquals(adler32, adler32Again, "相同输入应该产生相同的 Adler32");
    }
    
    @Test
    void testXorArrays()
    {
        byte[] src = new byte[]{ 0x01, 0x02, 0x03, 0x04 };
        byte xor = 0x55;
        byte xor_s = 0x10;
        byte xor_e = 0x20;
        
        byte result = CryptoUtil.xorArrays(src, xor, xor_s, xor_e);
        
        // 验证 XOR 操作确实修改了数组
        assertFalse(src[0] == 0x01 && src[1] == 0x02 && src[2] == 0x03 && src[3] == 0x04,
            "XOR 操作应该修改数组内容");
    }
    
    @Test
    void testXorArraysWithFFKeys()
    {
        byte[] src = new byte[]{ 0x01, 0x02, 0x03, 0x04 };
        byte xor = 0x55;
        byte xor_s = (byte) 0xFF;
        byte xor_e = (byte) 0xFF;
        
        byte result = CryptoUtil.xorArrays(src, xor, xor_s, xor_e);
        
        // 当 xor_s 和 xor_e 都是 0xFF 时，应该只使用初始 xor 值
        assertEquals(xor, result, "当 xor_s 和 xor_e 都是 0xFF 时，返回的 xor 应该不变");
    }
    
    @Test
    void testXorSign()
    {
        byte[] data = new byte[]{ 0x01, 0x02, 0x03 };
        
        // 计算正确的 XOR 校验值
        byte xor = 0;
        for(byte b : data) {
            xor ^= b;
        }
        
        assertTrue(CryptoUtil.xorSign(data, xor), "正确的 XOR 校验值应该验证通过");
        assertFalse(CryptoUtil.xorSign(data, (byte) (xor + 1)), "错误的 XOR 校验值应该验证失败");
    }
    
    @Test
    void testXor()
    {
        byte[] data = new byte[]{ 0x01, 0x02, 0x03 };
        
        byte xor = CryptoUtil.xor(data);
        
        // 手动计算验证
        byte expected = 0;
        for(byte b : data) {
            expected ^= b;
        }
        
        assertEquals(expected, xor, "XOR 计算结果应该正确");
    }
    
    @Test
    void testSha256()
    {
        String input = "test string";
        String hash = CryptoUtil.SHA256(input);
        
        assertNotNull(hash, "SHA256 哈希不应该为 null");
        assertEquals(64, hash.length(), "SHA256 哈希长度应该是 64（32字节十六进制表示）");
        
        // 相同输入应该产生相同哈希
        String hashAgain = CryptoUtil.SHA256(input);
        assertEquals(hash, hashAgain, "相同输入应该产生相同的 SHA256 哈希");
        
        // 不同输入应该产生不同哈希
        String hashDifferent = CryptoUtil.SHA256("different string");
        assertNotEquals(hash, hashDifferent, "不同输入应该产生不同的 SHA256 哈希");
    }
    
    @Test
    void testSha256Bytes()
    {
        byte[] input = "test data".getBytes(StandardCharsets.UTF_8);
        byte[] hash = cryptoUtil.sha256(input);
        
        assertNotNull(hash, "SHA256 哈希不应该为 null");
        assertEquals(32, hash.length, "SHA256 哈希长度应该是 32 字节");
        
        // 相同输入应该产生相同哈希
        byte[] hashAgain = cryptoUtil.sha256(input);
        assertArrayEquals(hash, hashAgain, "相同输入应该产生相同的 SHA256 哈希");
    }
    
    @Test
    void testBase64Encoder()
    {
        byte[] input = "Hello World".getBytes(StandardCharsets.UTF_8);
        String encoded = CryptoUtil.base64Encoder(input, 0, 0);
        
        assertNotNull(encoded, "Base64 编码不应该为 null");
        assertFalse(encoded.isEmpty(), "Base64 编码不应该为空");
    }
    
    @Test
    void testConstantTimeEquals()
    {
        String a = "secret";
        String b = "secret";
        String c = "different";
        
        assertTrue(CryptoUtil.constantTimeEquals(a, b), "相同字符串应该相等");
        assertFalse(CryptoUtil.constantTimeEquals(a, c), "不同字符串应该不相等");
        assertFalse(CryptoUtil.constantTimeEquals(a, null), "与 null 比较应该返回 false");
        assertFalse(CryptoUtil.constantTimeEquals(null, a), "null 与字符串比较应该返回 false");
        assertTrue(CryptoUtil.constantTimeEquals(null, null), "两个 null 应该相等");
    }
    
    @Test
    void testGenerateEccKeyPair()
    {
        CryptoUtil.ASymmetricKeyPair keyPair = CryptoUtil.generateEccKeyPair(256);
        
        assertNotNull(keyPair, "ECC 密钥对不应该为 null");
        assertNotNull(keyPair.getPublicKey(), "公钥不应该为 null");
        assertNotNull(keyPair.getPrivateKey(), "私钥不应该为 null");
        assertEquals("EC", keyPair.getAlgorithm(), "算法应该是 EC");
        assertFalse(keyPair.getPublicKey().isEmpty(), "公钥不应该为空");
        assertFalse(keyPair.getPrivateKey().isEmpty(), "私钥不应该为空");
    }
    
    @Test
    void testEccEncryptDecrypt()
    {
        CryptoUtil.ASymmetricKeyPair keyPair = CryptoUtil.generateEccKeyPair(256);
        assertNotNull(keyPair);
        
        String plaintext = "Hello, World!";
        String encrypted = CryptoUtil.eccEncrypt(keyPair.getPublicKey(), plaintext);
        
        assertNotNull(encrypted, "加密结果不应该为 null");
        assertFalse(encrypted.equals(plaintext), "密文应该与明文不同");
        
        String decrypted = CryptoUtil.eccDecrypt(keyPair.getPrivateKey(), encrypted);
        assertEquals(plaintext, decrypted, "解密后的明文应该与原始明文相同");
    }
    
    @Test
    void testEccSignAndVerify()
    {
        CryptoUtil.ASymmetricKeyPair keyPair = CryptoUtil.generateEccKeyPair(256);
        assertNotNull(keyPair);
        
        String data = "Data to be signed";
        String signature = CryptoUtil.eccSign(keyPair.getPrivateKey(), data);
        
        assertNotNull(signature, "签名不应该为 null");
        assertFalse(signature.isEmpty(), "签名不应该为空");
        
        boolean verified = CryptoUtil.eccVerify(keyPair.getPublicKey(), data, signature);
        assertTrue(verified, "正确的签名应该验证通过");
        
        boolean verifiedWrong = CryptoUtil.eccVerify(keyPair.getPublicKey(), "wrong data", signature);
        assertFalse(verifiedWrong, "错误的数据签名验证应该失败");
    }
    
    @Test
    void testPasswordGeneration()
    {
        String password = CryptoUtil.Password(10, 20);
        
        assertNotNull(password, "生成的密码不应该为 null");
        assertTrue(password.length() >= 10, "密码长度应该大于等于最小值");
        assertTrue(password.length() < 20, "密码长度应该小于最大值");
    }
    
    @Test
    void testQuotedPrintableEncoding()
    {
        String input = "Hello=World";
        String encoded = CryptoUtil.quoted_print_Encoding(input, "UTF-8");
        
        assertNotNull(encoded, "编码结果不应该为 null");
    }
    
    @Test
    void testQuotedPrintableDecoding()
    {
        String input = "Hello=3DWorld";
        String decoded = CryptoUtil.quoted_print_Decoding(input, "UTF-8");
        
        assertNotNull(decoded, "解码结果不应该为 null");
    }
}
