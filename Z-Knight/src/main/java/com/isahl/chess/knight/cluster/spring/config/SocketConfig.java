/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.knight.cluster.spring.config;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.util.unit.DataSize;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.config.ISocketConfig;

public class SocketConfig
        implements
        ISocketConfig
{
    private boolean        keepAlive;
    private Duration       connectTimeoutInSecond;
    private Duration       writeTimeoutInSecond;
    private Duration       readTimeoutInMinute;
    private Duration       soLingerInSecond;
    private DataSize       sendBufferSize;
    private DataSize       recvBufferSize;
    private int            sendQueueMax;
    private boolean        tcpNoDelay;
    private String         keyStorePath;
    private String         trustKeyStorePath;
    private String         keyPassword;
    private String         trustKeyPassword;
    private KeyManager[]   keyManagers;
    private TrustManager[] trustManagers;
    private boolean        clientAuth;
    private int            sslPacketBufferSize;
    private int            sslAppBufferSize;

    @Override
    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    @Override
    public Duration getWriteTimeoutInSecond()
    {
        return writeTimeoutInSecond;
    }

    @Override
    public Duration getReadTimeoutInMinute()
    {
        return readTimeoutInMinute;
    }

    @Override
    public int getSendQueueMax()
    {
        return sendQueueMax;
    }

    @Override
    public int getRcvInByte()
    {
        return (int) recvBufferSize.toBytes();
    }

    @Override
    public int getSnfInByte()
    {
        return (int) sendBufferSize.toBytes();
    }

    @Override
    public boolean isTcpNoDelay()
    {
        return tcpNoDelay;
    }

    @Override
    public Duration getSoLingerInSecond()
    {
        return soLingerInSecond;
    }

    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }

    public void setWriteTimeoutInSecond(Duration writeTimeoutInSecond)
    {
        this.writeTimeoutInSecond = writeTimeoutInSecond;
    }

    public void setReadTimeoutInMinute(Duration readTimeoutInMinute)
    {
        this.readTimeoutInMinute = readTimeoutInMinute;
    }

    public void setSoLingerInSecond(Duration soLingerInSecond)
    {
        this.soLingerInSecond = soLingerInSecond;
    }

    public void setSendBufferSize(DataSize sendBufferSize)
    {
        this.sendBufferSize = sendBufferSize;
    }

    public void setRecvBufferSize(DataSize recvBufferSize)
    {
        this.recvBufferSize = recvBufferSize;
    }

    public void setSendQueueMax(int sendQueueMax)
    {
        this.sendQueueMax = sendQueueMax;
    }

    public void setTcpNoDelay(boolean tcpNoDelay)
    {
        this.tcpNoDelay = tcpNoDelay;
    }

    @Override
    public Duration getConnectTimeoutInSecond()
    {
        return connectTimeoutInSecond;
    }

    public void setConnectTimeoutInSecond(Duration connectTimeoutInSecond)
    {
        this.connectTimeoutInSecond = connectTimeoutInSecond;
    }

    public void setKeyStorePath(String keyStorePath)
    {
        this.keyStorePath = IoUtil.isBlank(keyStorePath) ? null
                                                         : keyStorePath;
    }

    private KeyStore loadKeyStore(String path, String password) throws KeyStoreException,
                                                                IOException,
                                                                NoSuchAlgorithmException,
                                                                CertificateException
    {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(getClass().getClassLoader()
                                .getResourceAsStream(path),
                      password.toCharArray());
        return keyStore;
    }

    public void setTrustKeyStorePath(String trustKeyStorePath)
    {
        this.trustKeyStorePath = IoUtil.isBlank(trustKeyStorePath) ? null
                                                                   : trustKeyStorePath;
    }

    public void setKeyPassword(String keyPassword)
    {
        this.keyPassword = keyPassword;
    }

    public void setTrustKeyPassword(String trustKeyPassword)
    {
        this.trustKeyPassword = trustKeyPassword;
    }

    @Override
    public TrustManager[] getTrustManagers()
    {
        if (trustManagers == null && trustKeyStorePath != null && trustKeyPassword != null) {
            try {
                KeyStore keyStore = loadKeyStore(trustKeyStorePath, trustKeyPassword);
                TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
                factory.init(keyStore);
                return trustManagers = factory.getTrustManagers();
            }
            catch (KeyStoreException |
                   IOException |
                   NoSuchAlgorithmException |
                   CertificateException |
                   NoSuchProviderException e)
            {
                e.printStackTrace();
                return null;
            }
        }
        return trustManagers;
    }

    @Override
    public KeyManager[] getKeyManagers()
    {
        if (keyManagers == null && keyStorePath != null && keyPassword != null) {
            try {
                KeyStore keyStore = loadKeyStore(keyStorePath, keyPassword);
                KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
                factory.init(keyStore, keyPassword.toCharArray());
                return keyManagers = factory.getKeyManagers();
            }
            catch (KeyStoreException |
                   IOException |
                   NoSuchAlgorithmException |
                   CertificateException |
                   NoSuchProviderException |
                   UnrecoverableKeyException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        return keyManagers;
    }

    @Override
    public void init()
    {
        try {
            SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
            sslCtx.init(getKeyManagers(), getTrustManagers(), null);
            SSLSession sslSession = sslCtx.createSSLEngine()
                                          .getSession();
            sslPacketBufferSize = sslSession.getPacketBufferSize();
            sslAppBufferSize = sslSession.getApplicationBufferSize();
        }
        catch (NoSuchAlgorithmException |
               KeyManagementException e)
        {
            throw new ZException(e, "ssl static init failed");
        }
    }

    @Override
    public int getSslPacketBufferSize()
    {
        return sslPacketBufferSize;
    }

    @Override
    public int getSslAppBufferSize()
    {
        return sslAppBufferSize;
    }

    public void setClientAuth(boolean auth)
    {
        clientAuth = auth;
    }

    @Override
    public boolean isClientAuth()
    {
        return clientAuth;
    }
}
