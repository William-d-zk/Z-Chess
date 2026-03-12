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

package com.isahl.chess.king.base.crypt.util;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.crypt.features.ISymmetric;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * AES-GCM 对称加密实现（线程安全版本）
 * 
 * 替代已移除的 RC4 算法，提供更强的安全性。
 * 
 * 特性：
 * - 使用 AES-256-GCM 算法
 * - 自动 nonce 生成（每次加密使用随机 IV）
 * - 认证加密（防止篡改）
 * - 线程安全（每次加密/解密创建新的 Cipher 实例）
 * 
 * 安全警告：
 * - 每次加密必须使用不同的 IV/nonce
 * - 密文格式: IV(12 bytes) + Ciphertext + Auth Tag(16 bytes)
 * 
 * @author Z-Chess Team
 */
public class AesGcm implements ISymmetric {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96 bits - GCM 标准 IV 长度
    private static final int GCM_TAG_LENGTH = 128; // 128 bits - 认证标签长度
    private static final int AES_KEY_SIZE = 32;    // 256 bits - AES-256 密钥长度
    private static final int TAG_BYTES = GCM_TAG_LENGTH / 8; // 16 bytes

    // SecureRandom 是线程安全的
    private final SecureRandom _Random = new SecureRandom();

    public AesGcm() {
        // 验证算法可用性
        try {
            Cipher.getInstance(TRANSFORMATION);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM algorithm not available", e);
        }
    }

    @Override
    public byte[] createKey(String seed) {
        if (seed == null || seed.isEmpty()) {
            throw new IllegalArgumentException("Seed cannot be null or empty");
        }
        try {
            // 使用 SHA-256 生成固定长度的密钥
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // 取前 32 字节作为 AES-256 密钥
            byte[] key = new byte[AES_KEY_SIZE];
            System.arraycopy(hash, 0, key, 0, AES_KEY_SIZE);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create key from seed", e);
        }
    }

    @Override
    public void digest(ByteBuf dst, byte[] key) {
        if (dst == null || key == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        if (key.length != AES_KEY_SIZE) {
            throw new IllegalArgumentException("Key must be " + AES_KEY_SIZE + " bytes for AES-256");
        }
        
        // 每次加密创建新的 Cipher 实例（线程安全）
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            _Random.nextBytes(iv);
            
            SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] plaintext = new byte[dst.readableBytes()];
            dst.get(plaintext);
            
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // 输出格式: IV (12 bytes) + Ciphertext + Auth Tag (16 bytes)
            dst.clear();
            dst.put(iv);
            dst.put(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    @Override
    public void digest(byte[] dst, byte[] key) {
        if (dst == null || key == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        ByteBuf buf = new ByteBuf(dst.length, false);
        buf.put(dst);
        digest(buf, key);
        byte[] result = new byte[buf.readableBytes()];
        buf.get(result);
        System.arraycopy(result, 0, dst, 0, Math.min(result.length, dst.length));
    }

    /**
     * 解密数据
     * 
     * @param encryptedData 加密数据（格式: IV + Ciphertext + Auth Tag）
     * @param key 解密密钥（必须是 32 字节）
     * @return 明文数据
     * @throws IllegalArgumentException 参数无效时抛出
     * @throws RuntimeException 解密失败时抛出（可能是数据被篡改）
     */
    public byte[] decrypt(byte[] encryptedData, byte[] key) {
        if (encryptedData == null || key == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        if (key.length != AES_KEY_SIZE) {
            throw new IllegalArgumentException("Key must be " + AES_KEY_SIZE + " bytes for AES-256");
        }
        if (encryptedData.length < GCM_IV_LENGTH + TAG_BYTES) {
            throw new IllegalArgumentException("Encrypted data too short");
        }
        
        // 每次解密创建新的 Cipher 实例（线程安全）
        try {
            // 提取 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            
            // 提取密文（包含认证标签）
            int cipherLen = encryptedData.length - GCM_IV_LENGTH;
            byte[] cipherBytes = new byte[cipherLen];
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherBytes, 0, cipherLen);
            
            SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            return cipher.doFinal(cipherBytes);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new RuntimeException("Decryption failed: data integrity check failed (possible tampering)", e);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /**
     * 获取密文长度（给定明文长度）
     * 
     * @param plaintextLength 明文长度
     * @return 密文长度（IV + ciphertext + auth tag）
     */
    public static int getCiphertextLength(int plaintextLength) {
        return GCM_IV_LENGTH + plaintextLength + TAG_BYTES;
    }

    /**
     * 生成随机密钥
     * 
     * @return 32 字节随机密钥
     */
    public byte[] generateRandomKey() {
        byte[] key = new byte[AES_KEY_SIZE];
        _Random.nextBytes(key);
        return key;
    }

    @Override
    public void reset() {
        // 无需重置，Cipher 实例在每次使用时创建
    }
}
