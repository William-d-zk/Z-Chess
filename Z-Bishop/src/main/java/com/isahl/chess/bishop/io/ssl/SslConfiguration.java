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

package com.isahl.chess.bishop.io.ssl;

import com.isahl.chess.king.base.log.Logger;

/**
 * SSL 配置类
 * 支持通过系统属性配置 SSL Provider 行为
 * 
 * 配置项:
 * - ssl.provider.force: 强制使用指定 provider (wolfssl/jdk)
 * - ssl.provider.disableWolfSSL: 禁用 WolfSSL (true/false)
 * - ssl.debug: 开启 SSL 调试信息 (true/false)
 * 
 * @author william.d.zk
 */
public class SslConfiguration {

    private static final Logger _Logger = Logger.getLogger(SslConfiguration.class.getSimpleName());

    // 系统属性名称
    public static final String PROP_FORCE_PROVIDER = "ssl.provider.force";
    public static final String PROP_DISABLE_WOLFSSL = "ssl.provider.disableWolfSSL";
    public static final String PROP_SSL_DEBUG = "ssl.debug";
    public static final String PROP_WOLFSSL_DEBUG = "wolfssl.debug";

    /**
     * 获取强制指定的 SSL Provider
     * @return 强制指定的 Provider 类型，如果没有指定则返回 null
     */
    public static SslProviderFactory.SslProviderType getForcedProvider() {
        String forceProvider = System.getProperty(PROP_FORCE_PROVIDER);
        if (forceProvider == null || forceProvider.isEmpty()) {
            return null;
        }

        switch (forceProvider.toLowerCase()) {
            case "wolfssl":
            case "wolf":
                return SslProviderFactory.SslProviderType.WOLFSSL;
            case "jdk":
            case "default":
                return SslProviderFactory.SslProviderType.JDK;
            default:
                _Logger.warning("Unknown SSL provider '%s' specified in %s, using auto-detect", 
                              forceProvider, PROP_FORCE_PROVIDER);
                return null;
        }
    }

    /**
     * 检查是否禁用了 WolfSSL
     * @return 是否禁用
     */
    public static boolean isWolfSSLDisabled() {
        return Boolean.getBoolean(PROP_DISABLE_WOLFSSL);
    }

    /**
     * 检查是否开启了 SSL 调试
     * @return 是否开启
     */
    public static boolean isSslDebugEnabled() {
        return Boolean.getBoolean(PROP_SSL_DEBUG);
    }

    /**
     * 检查是否开启了 WolfSSL 调试
     * @return 是否开启
     */
    public static boolean isWolfSSLDebugEnabled() {
        return Boolean.getBoolean(PROP_WOLFSSL_DEBUG);
    }

    /**
     * 应用 SSL 调试配置
     */
    public static void applyDebugSettings() {
        if (isSslDebugEnabled()) {
            System.setProperty("javax.net.debug", "ssl,handshake");
            _Logger.info("SSL debug enabled: javax.net.debug=ssl,handshake");
        }
        
        if (isWolfSSLDebugEnabled()) {
            System.setProperty("wolfssl.debug", "true");
            _Logger.info("WolfSSL debug enabled");
        }
    }

    /**
     * 打印当前 SSL 配置
     */
    public static void printConfiguration() {
        _Logger.info("===== SSL Configuration =====");
        _Logger.info("Force Provider: %s", System.getProperty(PROP_FORCE_PROVIDER, "[not set, auto-detect]"));
        _Logger.info("Disable WolfSSL: %s", isWolfSSLDisabled());
        _Logger.info("SSL Debug: %s", isSslDebugEnabled());
        _Logger.info("WolfSSL Debug: %s", isWolfSSLDebugEnabled());
        _Logger.info("=============================");
    }
}
