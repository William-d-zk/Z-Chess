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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 压力测试验证器单元测试
 * 
 * 使用示例：
 * 
 * 1. 基础测试（3000 并发，60 秒）
 *    - 目标：本地 MQTT broker (127.0.0.1:1883)
 *    - QPS：30000（每个连接 10 req/s）
 * 
 * 2. 自定义配置测试
 *    - 修改并发数、目标地址、协议类型等参数
 */
@SpringBootTest
public class PressureTestValidatorTest
{
    /**
     * 示例：基础压力测试
     * 
     * 测试 3000 并发客户端，持续 60 秒
     * 目标 QPS：30,000
     */
    @Test
    public void testBasicPressure() throws Exception
    {
        // 创建配置
        PressureTestConfig config = new PressureTestConfig();
        config.setConcurrency(3000);
        config.setDuration(Duration.ofSeconds(60));
        config.setRequestsPerSecondPerClient(10);
        config.getTarget().setHost("127.0.0.1");
        config.getTarget().setPort(1883);
        config.setProtocol("mqtt");
        config.setVerbose(false);

        System.out.println("Test config: " + config);
        System.out.println("Target QPS: " + config.getTargetTotalQps());

        // 注意：实际测试需要注入 PressureTestValidator
        // 这里仅展示配置方式
    }

    /**
     * 示例：小规模快速测试
     * 
     * 100 并发，10 秒，用于快速验证
     */
    @Test
    public void testQuickSmoke() throws Exception
    {
        PressureTestConfig config = new PressureTestConfig();
        config.setConcurrency(100);
        config.setDuration(Duration.ofSeconds(10));
        config.setRequestsPerSecondPerClient(5);
        config.getTarget().setHost("127.0.0.1");
        config.getTarget().setPort(1883);
        config.setProtocol("mqtt");
        config.setWarmUpSeconds(2);

        System.out.println("Quick test config: " + config);
    }

    /**
     * 示例：WebSocket 压力测试
     */
    @Test
    public void testWebSocketPressure() throws Exception
    {
        PressureTestConfig config = new PressureTestConfig();
        config.setConcurrency(1000);
        config.setDuration(Duration.ofSeconds(60));
        config.setRequestsPerSecondPerClient(20);
        config.getTarget().setHost("127.0.0.1");
        config.getTarget().setPort(8080);
        config.getTarget().setPath("/ws");
        config.setProtocol("websocket");
        config.setPayloadSize(512);

        System.out.println("WebSocket test config: " + config);
    }

    /**
     * 示例：高吞吐测试
     * 
     * 5000 并发，100 req/s 每连接
     * 目标 QPS：500,000
     */
    @Test
    public void testHighThroughput() throws Exception
    {
        PressureTestConfig config = new PressureTestConfig();
        config.setConcurrency(5000);
        config.setDuration(Duration.ofSeconds(120));
        config.setRequestsPerSecondPerClient(100);
        config.getTarget().setHost("127.0.0.1");
        config.getTarget().setPort(1883);
        config.setProtocol("mqtt");
        config.setPayloadSize(128);  // 小 payload 测试纯吞吐
        config.setConnectionRate(200);

        System.out.println("High throughput test config: " + config);
        System.out.println("Target QPS: " + config.getTargetTotalQps());
    }

    /**
     * 示例：使用 API 控制测试
     * 
     * 演示如何通过编程方式控制测试生命周期
     */
    @Test
    public void testProgrammaticControl() throws Exception
    {
        // 伪代码示例：
        
        // 1. 启动测试
        // CompletableFuture<PressureMetrics.Snapshot> future = validator.startTest();
        
        // 2. 等待测试完成或超时
        // PressureMetrics.Snapshot result = future.get(70, TimeUnit.SECONDS);
        
        // 3. 输出结果
        // System.out.println(result.toReport());
        
        // 4. 或中途停止
        // validator.stopTest();
    }

    /**
     * 示例：实时指标监控
     */
    @Test
    public void testRealTimeMetrics() throws Exception
    {
        // 伪代码示例：
        
        // validator.setOnStatsUpdate(snapshot -> {
        //     System.out.printf("QPS: %.1f, Success: %.2f%%, Latency: %.2fms%n",
        //         snapshot.qps, snapshot.successRate, snapshot.avgLatencyMs);
        // });
        
        // validator.startTest();
    }
}
