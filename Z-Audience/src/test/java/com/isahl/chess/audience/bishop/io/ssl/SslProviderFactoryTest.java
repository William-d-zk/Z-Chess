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

package com.isahl.chess.bishop.io.ssl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.net.ssl.SSLContext;

/**
 * SSL Provider Factory 测试类
 * 
 * @author william.d.zk
 */
public class SslProviderFactoryTest {

    @Test
    void testInitialization() {
        // 测试初始化
        SslProviderFactory.initialize();
        assertNotNull(SslProviderFactory.getCurrentProvider());
    }

    @Test
    void testGetCurrentProvider() {
        // 测试获取当前 Provider
        SslProviderFactory.SslProviderType provider = SslProviderFactory.getCurrentProvider();
        assertNotNull(provider);
        
        // 验证是有效的 Provider 类型之一
        assertTrue(provider == SslProviderFactory.SslProviderType.WOLFSSL ||
                   provider == SslProviderFactory.SslProviderType.OPENSSL ||
                   provider == SslProviderFactory.SslProviderType.JDK);
    }

    @Test
    void testGetSSLContext() throws Exception {
        // 测试获取 SSLContext
        SSLContext context = SslProviderFactory.getSSLContext("TLSv1.2");
        assertNotNull(context);
        assertEquals("TLSv1.2", context.getProtocol());
    }

    @Test
    void testIsNativeSslAvailable() {
        // 测试原生 SSL 可用性检查
        boolean nativeAvailable = SslProviderFactory.isNativeSslAvailable();
        SslProviderFactory.SslProviderType provider = SslProviderFactory.getCurrentProvider();
        
        // 如果使用了原生库，provider 应该是 WOLFSSL 或 OPENSSL
        if (nativeAvailable) {
            assertTrue(provider == SslProviderFactory.SslProviderType.WOLFSSL ||
                      provider == SslProviderFactory.SslProviderType.OPENSSL);
        }
    }

    @Test
    void testIsWolfSSLAvailable() {
        // 测试 WolfSSL 可用性
        boolean wolfsslAvailable = SslProviderFactory.isWolfSSLAvailable();
        SslProviderFactory.SslProviderType provider = SslProviderFactory.getCurrentProvider();
        
        if (wolfsslAvailable) {
            assertEquals(SslProviderFactory.SslProviderType.WOLFSSL, provider);
        }
    }

    @Test
    void testIsOpenSSLAvailable() {
        // 测试 OpenSSL 可用性
        boolean opensslAvailable = SslProviderFactory.isOpenSSLAvailable();
        SslProviderFactory.SslProviderType provider = SslProviderFactory.getCurrentProvider();
        
        if (opensslAvailable) {
            assertEquals(SslProviderFactory.SslProviderType.OPENSSL, provider);
        }
    }

    @Test
    void testPrintStatus() {
        // 测试打印状态（不抛出异常即可）
        assertDoesNotThrow(() -> SslProviderFactory.printStatus());
    }

    @Test
    void testProviderTypeEnum() {
        // 测试 Provider 类型枚举
        SslProviderFactory.SslProviderType wolfssl = SslProviderFactory.SslProviderType.WOLFSSL;
        assertEquals("wolfJSSE", wolfssl.getProviderName());
        assertEquals("WolfSSL", wolfssl.getDisplayName());
        
        SslProviderFactory.SslProviderType openssl = SslProviderFactory.SslProviderType.OPENSSL;
        assertEquals("OpenSSL", openssl.getProviderName());
        // 实际返回可能包含额外信息如 "OpenSSL (via Netty TCNative)"
        assertTrue(openssl.getDisplayName().startsWith("OpenSSL"), 
            "OpenSSL display name should start with OpenSSL");
        
        SslProviderFactory.SslProviderType jdk = SslProviderFactory.SslProviderType.JDK;
        assertEquals("SunJSSE", jdk.getProviderName());
        assertEquals("JDK Default", jdk.getDisplayName());
    }
}
