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

package com.isahl.chess.knight.cluster.config;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.config.ISocketConfig;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import javax.net.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.unit.DataSize;

public class SocketConfig implements ISocketConfig {
  private final Logger _Logger =
      LoggerFactory.getLogger("cluster.knight." + getClass().getSimpleName());
  private boolean keepAlive;
  private Duration connectTimeoutInSecond;
  private Duration writeTimeoutInSecond;
  private Duration readTimeoutInMinute;
  private Duration soLingerInSecond;
  private DataSize sendBufferSize;
  private DataSize recvBufferSize;
  private int sendQueueMax;
  private boolean tcpNoDelay;
  private String keyStorePath;
  private String trustKeyStorePath;
  private String keyPassword;
  private String trustKeyPassword;
  private KeyManager[] keyManagers;
  private TrustManager[] trustManagers;
  private boolean clientAuth;
  private int sslPacketBufferSize;
  private int sslAppBufferSize;

  @Override
  public boolean isKeepAlive() {
    return keepAlive;
  }

  @Override
  public Duration getWriteTimeoutInSecond() {
    return writeTimeoutInSecond;
  }

  @Override
  public Duration getReadTimeoutInMinute() {
    return readTimeoutInMinute;
  }

  @Override
  public int getSendQueueMax() {
    return sendQueueMax;
  }

  @Override
  public int getRcvInByte() {
    return (int) recvBufferSize.toBytes();
  }

  @Override
  public int getSnfInByte() {
    return (int) sendBufferSize.toBytes();
  }

  @Override
  public boolean isTcpNoDelay() {
    return tcpNoDelay;
  }

  @Override
  public Duration getSoLingerInSecond() {
    return soLingerInSecond;
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  public void setWriteTimeoutInSecond(Duration writeTimeoutInSecond) {
    this.writeTimeoutInSecond = writeTimeoutInSecond;
  }

  public void setReadTimeoutInMinute(Duration readTimeoutInMinute) {
    this.readTimeoutInMinute = readTimeoutInMinute;
  }

  public void setSoLingerInSecond(Duration soLingerInSecond) {
    this.soLingerInSecond = soLingerInSecond;
  }

  public void setSendBufferSize(DataSize sendBufferSize) {
    this.sendBufferSize = sendBufferSize;
  }

  public void setRecvBufferSize(DataSize recvBufferSize) {
    this.recvBufferSize = recvBufferSize;
  }

  public void setSendQueueMax(int sendQueueMax) {
    this.sendQueueMax = sendQueueMax;
  }

  public void setTcpNoDelay(boolean tcpNoDelay) {
    this.tcpNoDelay = tcpNoDelay;
  }

  @Override
  public Duration getConnectTimeoutInSecond() {
    return connectTimeoutInSecond;
  }

  public void setConnectTimeoutInSecond(Duration connectTimeoutInSecond) {
    this.connectTimeoutInSecond = connectTimeoutInSecond;
  }

  public void setKeyStorePath(String keyStorePath) {
    this.keyStorePath = IoUtil.isBlank(keyStorePath) ? null : keyStorePath;
  }

  private KeyStore loadKeyStore(String path, String password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    _Logger.debug("Attempting to load keystore: %s", path);

    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    InputStream is = getClass().getClassLoader().getResourceAsStream(path);

    if (is == null) {
      _Logger.error("Keystore not found in classpath: %s", path);
      throw new IOException("Keystore not found: " + path);
    }

    try {
      keyStore.load(is, password.toCharArray());
      _Logger.info("Successfully loaded keystore: %s", path);
      return keyStore;
    } finally {
      is.close();
    }
  }

  public void setTrustKeyStorePath(String trustKeyStorePath) {
    this.trustKeyStorePath = IoUtil.isBlank(trustKeyStorePath) ? null : trustKeyStorePath;
  }

  public void setKeyPassword(String keyPassword) {
    this.keyPassword = keyPassword;
  }

  public void setTrustKeyPassword(String trustKeyPassword) {
    this.trustKeyPassword = trustKeyPassword;
  }

  @Override
  public TrustManager[] getTrustManagers() {
    if (trustManagers == null && trustKeyStorePath != null && trustKeyPassword != null) {
      try {
        KeyStore keyStore = loadKeyStore(trustKeyStorePath, trustKeyPassword);
        TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
        factory.init(keyStore);
        return trustManagers = factory.getTrustManagers();
      } catch (KeyStoreException
          | IOException
          | NoSuchAlgorithmException
          | CertificateException
          | NoSuchProviderException e) {
        _Logger.warn("Failed to load trust managers: %s", e.getMessage());
        return null;
      }
    }
    return trustManagers;
  }

  @Override
  public KeyManager[] getKeyManagers() {
    if (keyManagers == null && keyStorePath != null && keyPassword != null) {
      try {
        KeyStore keyStore = loadKeyStore(keyStorePath, keyPassword);
        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
        factory.init(keyStore, keyPassword.toCharArray());
        return keyManagers = factory.getKeyManagers();
      } catch (KeyStoreException
          | IOException
          | NoSuchAlgorithmException
          | CertificateException
          | NoSuchProviderException
          | UnrecoverableKeyException e) {
        _Logger.warn("Failed to load key managers: %s", e.getMessage());
        return null;
      }
    }

    return keyManagers;
  }

  @Override
  public void init() {
    _Logger.info("SocketConfig.init() started");
    try {
      if (keyStorePath != null) {
        _Logger.info("Loading keystore from: %s", keyStorePath);
        KeyManager[] kms = getKeyManagers();
        _Logger.info("Loaded %d key managers", kms != null ? kms.length : 0);
      }
      if (trustKeyStorePath != null) {
        _Logger.info("Loading truststore from: %s", trustKeyStorePath);
        TrustManager[] tms = getTrustManagers();
        _Logger.info("Loaded %d trust managers", tms != null ? tms.length : 0);
      }

      SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
      sslCtx.init(getKeyManagers(), getTrustManagers(), null);
      _Logger.info("SSLContext initialized successfully");

      SSLSession sslSession = sslCtx.createSSLEngine().getSession();
      sslPacketBufferSize = sslSession.getPacketBufferSize();
      sslAppBufferSize = sslSession.getApplicationBufferSize();
      _Logger.info("SSL buffer sizes - packet: %d, app: %d", sslPacketBufferSize, sslAppBufferSize);
    } catch (Exception e) {
      _Logger.error("SSL initialization failed: %s", e.getMessage());
      throw new ZException(e, "ssl static init failed");
    }
  }

  @Override
  public int getSslPacketBufferSize() {
    return sslPacketBufferSize;
  }

  @Override
  public int getSslAppBufferSize() {
    return sslAppBufferSize;
  }

  public void setClientAuth(boolean auth) {
    clientAuth = auth;
  }

  @Override
  public boolean isClientAuth() {
    return clientAuth;
  }
}
