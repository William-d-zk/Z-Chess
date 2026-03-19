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

import com.isahl.chess.audience.client.component.ClientPool;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.ws.ctrl.X101_HandShake;
import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.net.socket.AioSession;
import com.isahl.chess.queen.io.core.net.socket.BaseAioConnector;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnector;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 压力测试验证器主类
 *
 * <p>基于 AIO 的高并发压力测试验证器，支持： - 最高 5000 并发客户端连接 - MQTT/WebSocket/ZChat 多协议支持 - 动态连接速率控制 - 实时性能指标收集 -
 * 自动心跳维持
 *
 * <p>利用 Z-Queen 的 AIO 能力，避免为每个连接创建线程， 实现单机 3000+ 并发的高效压测。
 */
@Component
public class PressureTestValidator implements IValid {
  private static final Logger _Logger = Logger.getLogger("stress.validator");

  // 依赖注入
  private final PressureTestConfig config;
  private final IAioConfig aioConfig;
  private final ClientPool clientPool;
  private final TimeWheel timeWheel;

  // 性能指标收集器
  private final PressureMetrics metrics = new PressureMetrics();

  // 客户端管理
  private final Map<Long, StressClient> clients = new ConcurrentHashMap<>();
  private final AtomicLong clientIdGenerator = new AtomicLong(0);

  // 测试状态
  private volatile TestState state = TestState.IDLE;
  private volatile ICancelable statsTask;
  private volatile ICancelable durationTask;

  // 连接建立控制
  private final AtomicInteger connectingCount = new AtomicInteger(0);
  private volatile ICancelable connectTask;

  // 异步执行器（用于回调处理）
  private final ExecutorService callbackExecutor =
      Executors.newFixedThreadPool(
          4,
          r -> {
            Thread t = new Thread(r, "stress-callback-" + System.nanoTime());
            t.setDaemon(true);
            return t;
          });

  // 回调函数
  private Consumer<PressureMetrics.Snapshot> onStatsUpdate;
  private Consumer<PressureMetrics.Snapshot> onTestComplete;

  /** 测试状态枚举 */
  public enum TestState {
    IDLE, // 空闲
    CONNECTING, // 建立连接中
    WARMING_UP, // 预热中
    RUNNING, // 运行中
    COOLING_DOWN, // 冷却中
    COMPLETED, // 已完成
    ERROR // 错误
  }

  @Autowired
  public PressureTestValidator(
      PressureTestConfig config,
      @Qualifier("io_consumer_config") IAioConfig aioConfig,
      ClientPool clientPool) {
    this.config = config;
    this.aioConfig = aioConfig;
    this.clientPool = clientPool;
    this.timeWheel = clientPool.getTimeWheel();
  }

  @PostConstruct
  public void init() {
    _Logger.info("PressureTestValidator initialized: %s", config);
  }

  // ==================== 测试控制 ====================

  /** 启动压力测试 */
  public synchronized CompletableFuture<PressureMetrics.Snapshot> startTest() {
    if (state != TestState.IDLE && state != TestState.COMPLETED) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Test is already running, current state: " + state));
    }

    _Logger.info("Starting pressure test with config: %s", config);

    // 重置状态
    reset();
    state = TestState.CONNECTING;
    metrics.startTest();

    CompletableFuture<PressureMetrics.Snapshot> future = new CompletableFuture<>();

    // 启动统计任务
    startStatsTask();

    // 开始建立连接（控制速率）
    startConnectionPhase(
        () -> {
          // 连接建立完成后进入预热阶段
          state = TestState.WARMING_UP;
          _Logger.info(
              "All connections established, warming up for %d seconds...",
              config.getWarmUpSeconds());

          // 预热结束后开始正式测试
          timeWheel.acquire(
              this,
              new ScheduleHandler<PressureTestValidator>(
                  Duration.ofSeconds(config.getWarmUpSeconds()),
                  v -> {
                    state = TestState.RUNNING;
                    _Logger.info("Warm up completed, test is now running!");

                    // 启动所有客户端发送请求
                    clients.values().forEach(StressClient::startSending);

                    // 设置测试结束定时器
                    startDurationTimer(future);
                  }));
        });

    return future;
  }

  /** 停止压力测试 */
  public synchronized void stopTest() {
    if (state == TestState.IDLE || state == TestState.COMPLETED) {
      return;
    }

    _Logger.info("Stopping pressure test...");
    state = TestState.COOLING_DOWN;

    // 停止定时任务
    stopTasks();

    // 停止所有客户端
    clients.values().forEach(StressClient::stop);

    // 延迟关闭连接，让正在处理的请求完成
    timeWheel.acquire(
        this,
        new ScheduleHandler<PressureTestValidator>(
            Duration.ofSeconds(2),
            v -> {
              closeAllConnections();
              completeTest();
            }));
  }

  /** 强制停止测试（立即关闭所有连接） */
  public synchronized void forceStopTest() {
    _Logger.warning("Force stopping pressure test!");
    state = TestState.ERROR;

    stopTasks();
    closeAllConnections();
    completeTest();
  }

  // ==================== 连接管理 ====================

  /** 启动连接建立阶段（控制连接速率） */
  private void startConnectionPhase(Runnable onComplete) {
    final int totalClients = config.getConcurrency();
    final int rate = config.getConnectionRate();
    final AtomicInteger connected = new AtomicInteger(0);
    final AtomicInteger failed = new AtomicInteger(0);

    _Logger.info("Starting connection phase: target=%d clients, rate=%d/s", totalClients, rate);

    // 使用定时任务控制连接建立速率
    connectTask =
        timeWheel.acquire(
            this,
            new ScheduleHandler<PressureTestValidator>(
                Duration.ofMillis(1000 / rate), // 根据速率计算间隔
                true,
                v -> {
                  if (connected.get() + failed.get() >= totalClients) {
                    // 所有连接已尝试建立
                    connectTask.cancel();
                    if (failed.get() == 0) {
                      _Logger.info("All %d connections established successfully", connected.get());
                    } else {
                      _Logger.warning(
                          "Connections: %d success, %d failed", connected.get(), failed.get());
                    }
                    onComplete.run();
                    return;
                  }

                  // 批量建立连接（每 100ms 建立一批）
                  int batchSize =
                      Math.min(rate / 10, totalClients - connected.get() - failed.get());
                  for (int i = 0; i < batchSize; i++) {
                    long clientId = clientIdGenerator.incrementAndGet();
                    connectingCount.incrementAndGet();

                    createAndConnectClient(
                        clientId,
                        client -> {
                          connected.incrementAndGet();
                          connectingCount.decrementAndGet();
                        },
                        error -> {
                          failed.incrementAndGet();
                          connectingCount.decrementAndGet();
                        });
                  }
                }));
  }

  /** 创建并连接客户端 */
  private void createAndConnectClient(
      long clientId, Consumer<StressClient> onConnected, Consumer<Throwable> onError) {
    StressClient client = new StressClient(clientId, config, metrics, timeWheel);
    clients.put(clientId, client);

    // 设置回调
    client.setOnConnectedCallback(
        c -> {
          callbackExecutor.submit(() -> onConnected.accept(c));
        });
    client.setOnDisconnectedCallback(
        c -> {
          // 自动重连或清理
          if (state == TestState.RUNNING) {
            clients.remove(clientId);
          }
        });
    client.setOnErrorCallback(
        c -> {
          callbackExecutor.submit(() -> onError.accept(new IOException("Client error")));
        });

    try {
      // 根据协议类型创建连接器
      IAioConnector connector = createConnector(client);
      clientPool.connect(connector);
    } catch (IOException e) {
      client.onConnectFailed(e);
      onError.accept(e);
    }
  }

  /** 创建 AIO 连接器 */
  private IAioConnector createConnector(StressClient client) {
    String host = config.getTarget().getHost();
    int port = config.getTarget().getPort();
    ZSortHolder sortHolder = getSortHolder();

    return new BaseAioConnector(
        host,
        port,
        aioConfig.getSocketConfig(sortHolder.getSlot()),
        c -> client.onConnectFailed(new IOException("Connection failed"))) {
      @Override
      public String getProtocol() {
        return sortHolder.getSort().getProtocol();
      }

      @Override
      public ISort.Mode getMode() {
        return ISort.Mode.LINK;
      }

      @Override
      public ISession create(AsynchronousSocketChannel socketChannel, IConnectActivity activity)
          throws IOException {
        return new AioSession<>(
            socketChannel, this, sortHolder.getSort(), activity, clientPool, false);
      }

      @Override
      public void onCreated(ISession session) {
        super.onCreated(session);
        clientPool.addSession(session);
        client.onConnected(session);
      }

      @Override
      public ITriple afterConnected(ISession session) {
        // 发送协议特定的连接握手
        IProtocol handshake = createHandshake(client.getClientId());
        if (handshake != null) {
          return new Triple<>(handshake, session, OperateType.SINGLE);
        }
        return null;
      }
    };
  }

  /** 获取协议排序器 */
  private ZSortHolder getSortHolder() {
    return switch (config.getProtocol().toLowerCase()) {
      case "mqtt", "qtt" -> ZSortHolder.QTT_CONSUMER;
      case "websocket", "ws" -> ZSortHolder.WS_PLAIN_TEXT_CONSUMER;
      case "zchat", "cluster" -> ZSortHolder.Z_CLUSTER_SYMMETRY;
      default -> ZSortHolder.QTT_CONSUMER;
    };
  }

  /** 创建协议握手包 */
  private IProtocol createHandshake(long clientId) {
    return switch (config.getProtocol().toLowerCase()) {
      case "mqtt", "qtt" -> {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setClientId(generateClientId(clientId));
        connect.setClean();
        connect.setKeepAlive(config.getHeartbeatInterval());
        yield connect;
      }
      case "websocket", "ws" -> {
        String host = config.getTarget().getHost();
        String path = config.getTarget().getPath();
        yield new X101_HandShake<>(host, "secKey", 13);
      }
      default -> null;
    };
  }

  /** 生成客户端 ID */
  private String generateClientId(long clientId) {
    return String.format("STRESS_%08X_%d", System.currentTimeMillis() / 1000, clientId);
  }

  /** 关闭所有连接 */
  private void closeAllConnections() {
    _Logger.info("Closing all %d connections...", clients.size());
    clients.values().forEach(StressClient::close);
    clients.clear();
  }

  // ==================== 定时任务 ====================

  private void startStatsTask() {
    statsTask =
        timeWheel.acquire(
            this,
            new ScheduleHandler<PressureTestValidator>(
                Duration.ofSeconds(config.getStatsInterval()),
                true,
                v -> {
                  PressureMetrics.Snapshot snapshot = metrics.getSnapshot();
                  _Logger.info("Stats: %s", snapshot);

                  if (onStatsUpdate != null) {
                    callbackExecutor.submit(() -> onStatsUpdate.accept(snapshot));
                  }
                }));
  }

  private void startDurationTimer(CompletableFuture<PressureMetrics.Snapshot> future) {
    durationTask =
        timeWheel.acquire(
            this,
            new ScheduleHandler<PressureTestValidator>(
                config.getDuration(),
                v -> {
                  _Logger.info("Test duration reached, stopping...");
                  stopTest();
                  future.complete(metrics.getSnapshot());
                }));
  }

  private void stopTasks() {
    if (statsTask != null) {
      statsTask.cancel();
      statsTask = null;
    }
    if (durationTask != null) {
      durationTask.cancel();
      durationTask = null;
    }
    if (connectTask != null) {
      connectTask.cancel();
      connectTask = null;
    }
  }

  // ==================== 测试完成 ====================

  private void completeTest() {
    metrics.endTest();
    state = TestState.COMPLETED;

    PressureMetrics.Snapshot finalSnapshot = metrics.getSnapshot();
    _Logger.info("Test completed!\n%s", finalSnapshot.toReport());

    if (onTestComplete != null) {
      callbackExecutor.submit(() -> onTestComplete.accept(finalSnapshot));
    }
  }

  private void reset() {
    metrics.reset();
    clients.clear();
    clientIdGenerator.set(0);
    connectingCount.set(0);
  }

  // ==================== Getters & Setters ====================

  public TestState getState() {
    return state;
  }

  public PressureMetrics getMetrics() {
    return metrics;
  }

  public int getActiveClientCount() {
    return (int) clients.values().stream().filter(StressClient::isConnected).count();
  }

  public int getTotalPendingRequests() {
    return clients.values().stream().mapToInt(StressClient::getPendingRequestCount).sum();
  }

  public void setOnStatsUpdate(Consumer<PressureMetrics.Snapshot> callback) {
    this.onStatsUpdate = callback;
  }

  public void setOnTestComplete(Consumer<PressureMetrics.Snapshot> callback) {
    this.onTestComplete = callback;
  }

  public PressureTestConfig getConfig() {
    return config;
  }
}
