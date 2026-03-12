/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.io.ssl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CertificateWatcher 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CertificateWatcherTest {

    @TempDir
    Path tempDir;

    private Path keyStorePath;
    private Path trustStorePath;
    private CertificateWatcher watcher;
    private AtomicInteger reloadCount;
    private CountDownLatch reloadLatch;

    @BeforeEach
    void setUp() throws IOException {
        keyStorePath = tempDir.resolve("keystore.p12");
        trustStorePath = tempDir.resolve("truststore.p12");
        
        // 创建初始文件
        Files.write(keyStorePath, "initial-keystore".getBytes());
        Files.write(trustStorePath, "initial-truststore".getBytes());
        
        reloadCount = new AtomicInteger(0);
        reloadLatch = new CountDownLatch(1);
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试创建监视器")
    void testCreateWatcher() throws IOException {
        watcher = new CertificateWatcher(
            keyStorePath.toString(),
            trustStorePath.toString(),
            v -> reloadCount.incrementAndGet()
        );
        
        assertNotNull(watcher);
        assertFalse(watcher.isRunning());
    }

    @Test
    @Order(2)
    @DisplayName("测试启动和停止监视器")
    void testStartStop() throws IOException {
        watcher = new CertificateWatcher(
            keyStorePath.toString(),
            null,
            v -> {}
        );
        
        assertFalse(watcher.isRunning());
        
        watcher.start();
        assertTrue(watcher.isRunning());
        
        watcher.stop();
        assertFalse(watcher.isRunning());
    }

    @Test
    @Order(3)
    @DisplayName("测试检测 KeyStore 变化")
    void testDetectKeyStoreChange() throws Exception {
        watcher = new CertificateWatcher(
            keyStorePath.toString(),
            null,
            v -> {
                reloadCount.incrementAndGet();
                reloadLatch.countDown();
            },
            100  // 100ms 防抖，用于测试
        );
        
        watcher.start();
        Thread.sleep(100);  // 等待监视器启动
        
        // 修改 KeyStore 文件
        Files.write(keyStorePath, "updated-keystore".getBytes(), 
                    StandardOpenOption.TRUNCATE_EXISTING);
        
        // 等待回调
        boolean triggered = reloadLatch.await(5, TimeUnit.SECONDS);
        
        assertTrue(triggered, "Reload callback should be triggered");
        assertEquals(1, reloadCount.get(), "Reload count should be 1");
    }

    @Test
    @Order(4)
    @DisplayName("测试防抖功能")
    void testDebounce() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);
        
        watcher = new CertificateWatcher(
            keyStorePath.toString(),
            null,
            v -> {
                count.incrementAndGet();
                latch.countDown();
            },
            500  // 500ms 防抖
        );
        
        watcher.start();
        Thread.sleep(100);
        
        // 快速多次修改文件
        for (int i = 0; i < 5; i++) {
            Files.write(keyStorePath, ("update-" + i).getBytes(),
                        StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(50);  // 小于防抖时间
        }
        
        // 等待防抖后的回调
        boolean triggered = latch.await(3, TimeUnit.SECONDS);
        
        assertTrue(triggered);
        // 由于防抖，应该只触发一次
        assertEquals(1, count.get(), "Should only trigger once due to debounce");
    }

    @Test
    @Order(5)
    @DisplayName("测试手动强制重载")
    void testForceReload() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);
        
        watcher = new CertificateWatcher(
            keyStorePath.toString(),
            null,
            v -> {
                count.incrementAndGet();
                latch.countDown();
            }
        );
        
        watcher.start();
        
        // 强制重载
        watcher.forceReload();
        
        boolean triggered = latch.await(1, TimeUnit.SECONDS);
        
        assertTrue(triggered);
        assertEquals(1, count.get());
    }

    @Test
    @Order(6)
    @DisplayName("测试空 KeyStore 路径应抛出异常")
    void testEmptyKeyStorePath() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CertificateWatcher(null, null, v -> {});
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new CertificateWatcher("", null, v -> {});
        });
    }
}
