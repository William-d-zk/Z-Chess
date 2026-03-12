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

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 证书文件监视器
 * 
 * 监视 KeyStore 和 TrustStore 文件的变化，当证书文件被修改时触发重新加载。
 * 
 * 特性：
 * - 基于 Java NIO WatchService，资源占用低
 * - 支持防抖处理（避免文件写入过程中的多次触发）
 * - 异步执行，不阻塞业务线程
 * - 使用 ScheduledExecutorService 代替 Thread.sleep，响应更快
 * 
 * @author william.d.zk
 */
public class CertificateWatcher {

    private static final Logger _Logger = Logger.getLogger("io.bishop.ssl.CertificateWatcher");
    
    // 默认防抖时间（毫秒）- 证书文件通常较大，需要足够时间完成写入
    private static final long DEFAULT_DEBOUNCE_MS = 5000;
    
    // 检查间隔（毫秒）- 使用 poll 的超时参数，无需单独线程睡眠
    private static final long POLL_TIMEOUT_MS = 1000;

    private final WatchService _WatchService;
    private final ScheduledExecutorService _Scheduler;
    private final Path _KeyStorePath;
    private final Path _TrustStorePath;
    private final Consumer<Void> _ReloadCallback;
    private final long _DebounceMs;
    
    // 记录文件最后修改时间，避免重复触发
    private volatile long _LastKeyStoreModified = 0;
    private volatile long _LastTrustStoreModified = 0;
    private volatile ScheduledFuture<?> _PendingReload;
    private volatile boolean _Running = false;

    /**
     * 创建证书监视器
     * 
     * @param keyStorePath KeyStore 文件路径
     * @param trustStorePath TrustStore 文件路径（可为 null）
     * @param reloadCallback 证书变化时的回调函数
     * @throws IOException 创建 WatchService 失败
     */
    public CertificateWatcher(String keyStorePath, String trustStorePath, 
                              Consumer<Void> reloadCallback) throws IOException {
        this(keyStorePath, trustStorePath, reloadCallback, DEFAULT_DEBOUNCE_MS);
    }

    /**
     * 创建证书监视器（自定义防抖时间）
     * 
     * @param keyStorePath KeyStore 文件路径
     * @param trustStorePath TrustStore 文件路径（可为 null）
     * @param reloadCallback 证书变化时的回调函数
     * @param debounceMs 防抖时间（毫秒）
     * @throws IOException 创建 WatchService 失败
     */
    public CertificateWatcher(String keyStorePath, String trustStorePath,
                              Consumer<Void> reloadCallback, long debounceMs) throws IOException {
        if (keyStorePath == null || keyStorePath.isEmpty()) {
            throw new IllegalArgumentException("KeyStore path cannot be null or empty");
        }
        
        _KeyStorePath = Paths.get(keyStorePath).toAbsolutePath().normalize();
        _TrustStorePath = trustStorePath != null ? 
            Paths.get(trustStorePath).toAbsolutePath().normalize() : null;
        _ReloadCallback = reloadCallback;
        _DebounceMs = debounceMs;
        
        // 创建 WatchService
        _WatchService = FileSystems.getDefault().newWatchService();
        
        // 创建单线程调度器
        _Scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cert-watcher");
            t.setDaemon(true);
            return t;
        });
        
        _Logger.info("CertificateWatcher created for KeyStore: %s", _KeyStorePath);
        if (_TrustStorePath != null) {
            _Logger.info("CertificateWatcher also watching TrustStore: %s", _TrustStorePath);
        }
    }

    /**
     * 启动监视
     */
    public synchronized void start() {
        if (_Running) {
            _Logger.warning("CertificateWatcher already started");
            return;
        }
        
        _Running = true;
        
        try {
            // 注册 KeyStore 目录监视
            Path keyStoreDir = _KeyStorePath.getParent();
            if (keyStoreDir != null) {
                keyStoreDir.register(_WatchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);
                _Logger.info("Started watching directory: %s", keyStoreDir);
            }
            
            // 如果 TrustStore 在不同目录，也需要注册
            if (_TrustStorePath != null) {
                Path trustStoreDir = _TrustStorePath.getParent();
                if (trustStoreDir != null && !trustStoreDir.equals(keyStoreDir)) {
                    trustStoreDir.register(_WatchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);
                    _Logger.info("Started watching TrustStore directory: %s", trustStoreDir);
                }
            }
            
            // 启动监视线程
            Thread watchThread = new Thread(this::watchLoop, "cert-watch-loop");
            watchThread.setDaemon(true);
            watchThread.start();
            
            _Logger.info("CertificateWatcher started successfully");
        } catch (IOException e) {
            _Running = false;
            _Logger.error("Failed to start CertificateWatcher: %s", e.getMessage());
            throw new RuntimeException("Failed to start certificate watcher", e);
        }
    }

    /**
     * 停止监视
     */
    public synchronized void stop() {
        if (!_Running) {
            return;
        }
        
        _Running = false;
        
        try {
            _WatchService.close();
        } catch (IOException e) {
            _Logger.warning("Error closing WatchService: %s", e.getMessage());
        }
        
        _Scheduler.shutdown();
        try {
            if (!_Scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                _Scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            _Scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        _Logger.info("CertificateWatcher stopped");
    }

    /**
     * 监视循环
     */
    private void watchLoop() {
        while (_Running) {
            try {
                WatchKey key = _WatchService.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (key == null) {
                    continue;
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 忽略溢出事件
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path affectedPath = ((Path) key.watchable()).resolve(filename);
                    
                    // 检查是否是监视的文件
                    if (affectedPath.equals(_KeyStorePath)) {
                        long now = System.currentTimeMillis();
                        // 简单的去重检查：如果距离上次触发时间太短，则忽略
                        if (now - _LastKeyStoreModified > 100) {
                            _LastKeyStoreModified = now;
                            _Logger.info("KeyStore file changed: %s", affectedPath);
                            triggerReload("KeyStore");
                        }
                    } else if (_TrustStorePath != null && affectedPath.equals(_TrustStorePath)) {
                        long now = System.currentTimeMillis();
                        if (now - _LastTrustStoreModified > 100) {
                            _LastTrustStoreModified = now;
                            _Logger.info("TrustStore file changed: %s", affectedPath);
                            triggerReload("TrustStore");
                        }
                    }
                }
                
                // 重置 key
                boolean valid = key.reset();
                if (!valid) {
                    _Logger.warning("WatchKey no longer valid, stopping watcher");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                _Logger.error("Error in watch loop: %s", e.getMessage());
            }
        }
    }

    /**
     * 触发重新加载（带防抖）
     */
    private void triggerReload(String source) {
        // 取消之前的待处理任务
        if (_PendingReload != null && !_PendingReload.isDone()) {
            _PendingReload.cancel(false);
            _Logger.debug("Cancelled previous reload task due to new %s change", source);
        }
        
        // 调度新的重新加载任务
        _PendingReload = _Scheduler.schedule(() -> {
            try {
                _Logger.info("Executing certificate reload (triggered by %s)...", source);
                _ReloadCallback.accept(null);
                _Logger.info("Certificate reload completed successfully");
            } catch (Exception e) {
                _Logger.error("Certificate reload failed: %s", e.getMessage());
            }
        }, _DebounceMs, TimeUnit.MILLISECONDS);
        
        _Logger.info("Scheduled certificate reload in %d ms (triggered by %s)", _DebounceMs, source);
    }

    /**
     * 强制立即重新加载（不等待防抖）
     */
    public void forceReload() {
        if (_PendingReload != null && !_PendingReload.isDone()) {
            _PendingReload.cancel(false);
        }
        
        _Scheduler.execute(() -> {
            try {
                _Logger.info("Executing forced certificate reload...");
                _ReloadCallback.accept(null);
                _Logger.info("Forced certificate reload completed");
            } catch (Exception e) {
                _Logger.error("Forced certificate reload failed: %s", e.getMessage());
            }
        });
    }

    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return _Running;
    }
}
