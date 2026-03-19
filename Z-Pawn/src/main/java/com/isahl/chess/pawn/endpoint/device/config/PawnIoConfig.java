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

package com.isahl.chess.pawn.endpoint.device.config;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.cluster.config.SocketConfig;
import com.isahl.chess.knight.cluster.config.SslSocketConfig;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.ISocketConfig;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author william.d.zk
 * @date 2020/4/24
 */
@Configuration("pawn_io_config")
@ConfigurationProperties(prefix = "z.chess.pawn.io")
@PropertySource("classpath:pawn.io.properties")
public class PawnIoConfig implements IAioConfig {
  private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

  private Map<String, Integer> sizePowers;

  private SocketConfig consumer;
  private SocketConfig internal;
  private SocketConfig cluster;
  private SocketConfig provider;

  // SslSocketConfig 用于TLS配置，优先使用
  private SslSocketConfig sslSocketConfig;

  @Override
  public boolean isDomainActive(int type) {
    return type <= ZUID.MAX_TYPE;
  }

  @Override
  public int getSizePower(int type) {
    return switch (type) {
      case ZUID.TYPE_CONSUMER_SLOT -> sizePowers.getOrDefault("consumer.0", 12);
      case ZUID.TYPE_INTERNAL_SLOT -> sizePowers.getOrDefault("internal.1", 7);
      case ZUID.TYPE_PROVIDER_SLOT -> sizePowers.getOrDefault("provider.2", 10);
      case ZUID.TYPE_CLUSTER_SLOT -> sizePowers.getOrDefault("cluster.3", 7);
      default -> throw new IllegalArgumentException();
    };
  }

  @Override
  public ISocketConfig getSocketConfig(int type) {
    // 优先使用 SslSocketConfig 的TLS配置（如果启用）
    _Logger.info("getSocketConfig called for type %d, sslSocketConfig=%s", type, sslSocketConfig);
    if (sslSocketConfig != null) {
      ISocketConfig sslConfig = getSslSocketConfig(type);
      if (sslConfig != null) {
        _Logger.info("Using SslSocketConfig for type %d", type);
        return sslConfig;
      }
    } else {
      _Logger.warning("sslSocketConfig is null, falling back to legacy SocketConfig");
    }

    // 回退到传统 SocketConfig
    _Logger.info("Using legacy SocketConfig for type %d", type);
    return switch (type) {
      case ZUID.TYPE_CONSUMER_SLOT -> consumer;
      case ZUID.TYPE_INTERNAL_SLOT -> internal;
      case ZUID.TYPE_PROVIDER_SLOT -> provider;
      case ZUID.TYPE_CLUSTER_SLOT -> cluster;
      default -> throw new IllegalArgumentException();
    };
  }

  /** 获取指定类型的 SSL 配置（如果启用） */
  private ISocketConfig getSslSocketConfig(int type) {
    return switch (type) {
      case ZUID.TYPE_CONSUMER_SLOT -> {
        if (sslSocketConfig.isSslEnabled("consumer")) {
          yield createSslAdapter(sslSocketConfig.getConsumer(), sslSocketConfig);
        }
        yield null;
      }
      case ZUID.TYPE_INTERNAL_SLOT -> {
        if (sslSocketConfig.isSslEnabled("internal")) {
          yield createSslAdapter(sslSocketConfig.getInternal(), sslSocketConfig);
        }
        yield null;
      }
      case ZUID.TYPE_PROVIDER_SLOT -> {
        if (sslSocketConfig.isSslEnabled("provider")) {
          yield createSslAdapter(sslSocketConfig.getProvider(), sslSocketConfig);
        }
        yield null;
      }
      case ZUID.TYPE_CLUSTER_SLOT -> {
        if (sslSocketConfig.isSslEnabled("cluster")) {
          yield createSslAdapter(sslSocketConfig.getCluster(), sslSocketConfig);
        }
        yield null;
      }
      default -> null;
    };
  }

  /** 创建 SSL 配置适配器，将 SslSocketConfig.SslConfig 适配为 ISocketConfig */
  private ISocketConfig createSslAdapter(
      SslSocketConfig.SslConfig sslConfig, SslSocketConfig parent) {
    return new ISocketConfig() {
      @Override
      public boolean isKeepAlive() {
        return true;
      }

      @Override
      public java.time.Duration getWriteTimeoutInSecond() {
        return java.time.Duration.ofSeconds(30);
      }

      @Override
      public java.time.Duration getReadTimeoutInMinute() {
        return java.time.Duration.ofMinutes(15);
      }

      @Override
      public int getSendQueueMax() {
        return 64;
      }

      @Override
      public int getRcvInByte() {
        return parent.getRcvInByte();
      }

      @Override
      public int getSnfInByte() {
        return parent.getSnfInByte();
      }

      @Override
      public boolean isTcpNoDelay() {
        return true;
      }

      @Override
      public java.time.Duration getSoLingerInSecond() {
        return java.time.Duration.ofSeconds(30);
      }

      @Override
      public java.time.Duration getConnectTimeoutInSecond() {
        return java.time.Duration.ofSeconds(5);
      }

      @Override
      public javax.net.ssl.KeyManager[] getKeyManagers() {
        return sslConfig.keyManagers;
      }

      @Override
      public javax.net.ssl.TrustManager[] getTrustManagers() {
        return sslConfig.trustManagers;
      }

      @Override
      public void init() {
        // SSL 上下文已在 SslSocketConfig 中初始化
      }

      @Override
      public int getSslPacketBufferSize() {
        return parent.getSslPacketBufferSize();
      }

      @Override
      public int getSslAppBufferSize() {
        return parent.getSslAppBufferSize();
      }

      @Override
      public boolean isClientAuth() {
        return sslConfig.isClientAuth();
      }
    };
  }

  @Autowired(required = false)
  public void setSslSocketConfig(SslSocketConfig sslSocketConfig) {
    this.sslSocketConfig = sslSocketConfig;
    if (sslSocketConfig != null) {
      _Logger.info("SslSocketConfig injected: %s", sslSocketConfig);
    } else {
      _Logger.warning("SslSocketConfig is null!");
    }
  }

  @PostConstruct
  public void init() {
    _Logger.info("PawnIoConfig initialized, sslSocketConfig=%s", sslSocketConfig);
  }

  public void setSizePowers(Map<String, Integer> sizePowers) {
    this.sizePowers = sizePowers;
  }

  public void setConsumer(SocketConfig consumer) {
    this.consumer = consumer;
  }

  public void setInternal(SocketConfig internal) {
    this.internal = internal;
  }

  public void setCluster(SocketConfig cluster) {
    this.cluster = cluster;
  }

  public void setProvider(SocketConfig provider) {
    this.provider = provider;
  }

  public SslSocketConfig getSslSocketConfig() {
    return sslSocketConfig;
  }
}
