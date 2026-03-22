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

package com.isahl.chess.audience.bishop.mqtt.v5;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.mqtt.v5.RedirectReason;
import com.isahl.chess.bishop.protocol.mqtt.MockNetworkOption;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11E_QttDisconnect;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol;
import com.isahl.chess.pawn.endpoint.device.service.LoadBalancer;
import com.isahl.chess.pawn.endpoint.device.service.MqttRedirectHandler;
import com.isahl.chess.pawn.endpoint.device.service.ServerRedirectService;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * MQTT 5.0 服务器重定向功能测试
 *
 * @author william.d.zk
 * @since 1.1.1
 */
class ServerRedirectTest {

  private QttContext createQttContext() {
    return new QttContext(new MockNetworkOption(), ISort.Mode.CLUSTER, ISort.Type.SERVER);
  }

  private ServerRedirectService redirectService;
  private LoadBalancer loadBalancer;
  private MqttRedirectHandler redirectHandler;

  @BeforeEach
  void setUp() {
    redirectService = new ServerRedirectService();
    loadBalancer = new LoadBalancer();
    redirectHandler = new MqttRedirectHandler();

    // 注入依赖
    redirectHandler.setRedirectService(redirectService);
    redirectHandler.setLoadBalancer(loadBalancer);

    // 配置当前服务器
    redirectService.setCurrentServerRef("localhost:1883");
    redirectService.setRedirectEnabled(true);

    // 添加目标服务器
    redirectService.addServer("server1:1883", 100);
    redirectService.addServer("server2:1883", 100);
  }

  // ==================== RedirectReason 测试 ====================

  @Test
  void testRedirectReasonCodes() {
    assertEquals((byte) 0x9C, RedirectReason.USE_ANOTHER_SERVER.getCode());
    assertEquals((byte) 0x9D, RedirectReason.SERVER_MOVED.getCode());

    assertEquals("Use Another Server", RedirectReason.USE_ANOTHER_SERVER.getDescription());
    assertEquals("Server Moved", RedirectReason.SERVER_MOVED.getDescription());

    assertFalse(RedirectReason.USE_ANOTHER_SERVER.isPermanent());
    assertTrue(RedirectReason.SERVER_MOVED.isPermanent());
  }

  @Test
  void testRedirectReasonFromCode() {
    assertEquals(RedirectReason.USE_ANOTHER_SERVER, RedirectReason.fromCode((byte) 0x9C));
    assertEquals(RedirectReason.SERVER_MOVED, RedirectReason.fromCode((byte) 0x9D));
    assertNull(RedirectReason.fromCode((byte) 0x00));
  }

  @Test
  void testRedirectReasonToCodeMqtt() {
    assertEquals(
        com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt.USE_ANOTHER_SERVER,
        RedirectReason.USE_ANOTHER_SERVER.toCodeMqtt());
    assertEquals(
        com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt.SERVER_MOVED,
        RedirectReason.SERVER_MOVED.toCodeMqtt());
  }

  // ==================== ServerRedirectService 测试 ====================

  @Test
  void testAddAndRemoveServer() {
    assertEquals(2, redirectService.getServers().size());

    redirectService.addServer("server3:1883", 50);
    assertEquals(3, redirectService.getServers().size());

    redirectService.removeServer("server3:1883");
    assertEquals(2, redirectService.getServers().size());
  }

  @Test
  void testServerHealth() {
    redirectService.setServerHealth("server1:1883", false);
    assertFalse(redirectService.isServerHealthy("server1:1883"));
    assertTrue(redirectService.isServerHealthy("server2:1883"));
    assertEquals(1, redirectService.getHealthyServerCount());
  }

  @Test
  void testDecideRedirect() {
    ServerRedirectService.RedirectDecision decision = redirectService.decideRedirect("client1", 0);

    assertNotNull(decision);
    assertEquals(RedirectReason.USE_ANOTHER_SERVER, decision.getReason());
    assertNotNull(decision.getTargetServer());
    assertFalse(decision.getTargetServer().equals("localhost:1883"));
    assertEquals(1, decision.getRedirectCount());
    assertFalse(decision.isPermanent());
  }

  @Test
  void testDecideRedirectDisabled() {
    redirectService.setRedirectEnabled(false);
    ServerRedirectService.RedirectDecision decision = redirectService.decideRedirect("client1", 0);
    assertNull(decision);
  }

  @Test
  void testDecideRedirectMaxHops() {
    ServerRedirectService.RedirectDecision decision =
        redirectService.decideRedirect("client1", ServerRedirectService.MAX_REDIRECT_HOPS);
    assertNull(decision);
  }

  @Test
  void testMaintenanceRedirect() {
    ServerRedirectService.RedirectDecision decision =
        redirectService.decideMaintenanceRedirect("client1", 0);

    assertNotNull(decision);
    assertEquals(RedirectReason.SERVER_MOVED, decision.getReason());
    assertTrue(decision.isPermanent());
  }

  @Test
  void testMigrationRedirect() {
    ServerRedirectService.RedirectDecision decision =
        redirectService.decideMigrationRedirect("newserver:1883", 0);

    assertNotNull(decision);
    assertEquals(RedirectReason.SERVER_MOVED, decision.getReason());
    assertEquals("newserver:1883", decision.getTargetServer());
  }

  // ==================== LoadBalancer 测试 ====================

  @Test
  void testRoundRobinSelection() {
    loadBalancer.addServer("server1:1883", 100);
    loadBalancer.addServer("server2:1883", 100);

    String server1 = loadBalancer.selectServer("client1");
    String server2 = loadBalancer.selectServer("client2");

    assertNotNull(server1);
    assertNotNull(server2);
  }

  @Test
  void testWeightedRoundRobin() {
    loadBalancer.setStrategy(LoadBalancer.Strategy.WEIGHTED_ROUND_ROBIN);
    loadBalancer.addServer("server1:1883", 80);
    loadBalancer.addServer("server2:1883", 20);

    // 多次选择，验证加权效果
    int count1 = 0, count2 = 0;
    for (int i = 0; i < 100; i++) {
      String server = loadBalancer.selectServer("client" + i);
      if ("server1:1883".equals(server)) {
        count1++;
      } else if ("server2:1883".equals(server)) {
        count2++;
      }
    }

    // server1 权重更高，应该被选择更多次
    assertTrue(count1 > count2);
  }

  @Test
  void testLeastConnections() {
    loadBalancer.setStrategy(LoadBalancer.Strategy.LEAST_CONNECTIONS);
    loadBalancer.addServer("server1:1883", 100);
    loadBalancer.addServer("server2:1883", 100);

    // 增加 server1 的连接数
    loadBalancer.incrementConnections("server1:1883");
    loadBalancer.incrementConnections("server1:1883");

    String selected = loadBalancer.selectServer("client1");
    // 应该选择连接数较少的 server2
    assertEquals("server2:1883", selected);
  }

  @Test
  void testConsistentHashing() {
    loadBalancer.setStrategy(LoadBalancer.Strategy.CONSISTENT_HASHING);
    loadBalancer.addServer("server1:1883", 100);
    loadBalancer.addServer("server2:1883", 100);

    // 相同客户端 ID 应该总是选择同一服务器
    String selected1 = loadBalancer.selectServer("client-fixed");
    String selected2 = loadBalancer.selectServer("client-fixed");
    String selected3 = loadBalancer.selectServer("client-fixed");

    assertEquals(selected1, selected2);
    assertEquals(selected2, selected3);
  }

  @Test
  void testConnectionCount() {
    loadBalancer.addServer("server1:1883", 100);

    assertEquals(0, loadBalancer.getConnectionCount("server1:1883"));

    loadBalancer.incrementConnections("server1:1883");
    assertEquals(1, loadBalancer.getConnectionCount("server1:1883"));

    loadBalancer.incrementConnections("server1:1883");
    loadBalancer.decrementConnections("server1:1883");
    assertEquals(1, loadBalancer.getConnectionCount("server1:1883"));
  }

  // ==================== X11E_QttDisconnect 测试 ====================

  @Test
  void testDisconnectProperties() {
    X11E_QttDisconnect disconnect = new X11E_QttDisconnect();

    // 设置 MQTT v5 上下文
    QttContext context = createQttContext();
    context.setVersion(QttProtocol.VERSION_V5_0);
    disconnect.wrap(createQttContext());

    disconnect.setCode((byte) 0x9C);
    disconnect.setServerReference("newserver:1883");
    disconnect.setReasonString("Server redirect");

    assertEquals(0x9C, disconnect.getCode());
    assertEquals("newserver:1883", disconnect.getServerReference());
    assertEquals("Server redirect", disconnect.getReasonString());
    assertTrue(disconnect.isRedirect());
  }

  @Test
  void testDisconnectNotRedirect() {
    X11E_QttDisconnect disconnect = new X11E_QttDisconnect();

    QttContext context = createQttContext();
    context.setVersion(QttProtocol.VERSION_V5_0);
    disconnect.wrap(createQttContext());

    disconnect.setNormalDisconnection();

    assertEquals(0x00, disconnect.getCode());
    assertFalse(disconnect.isRedirect());
  }

  @Test
  void testDisconnectReasonCodes() {
    X11E_QttDisconnect disconnect = new X11E_QttDisconnect();

    QttContext context = createQttContext();
    context.setVersion(QttProtocol.VERSION_V5_0);
    disconnect.wrap(createQttContext());

    disconnect.setNormalDisconnection();
    assertEquals(0x00, disconnect.getCode());

    disconnect.setSessionTakenOver();
    assertEquals(0x8E, disconnect.getCode());

    disconnect.setKeepAliveTimeout();
    assertEquals(0x8D, disconnect.getCode());

    disconnect.setServerShuttingDown();
    assertEquals(0x8B, disconnect.getCode());
  }

  // ==================== MqttRedirectHandler 测试 ====================

  @Test
  void testHandleConnectRedirect() {
    // 添加更多服务器确保有可用目标
    redirectService.addServer("server3:1883", 100);
    redirectService.setServerHealth("server1:1883", true);
    redirectService.setServerHealth("server2:1883", true);
    redirectService.setServerHealth("server3:1883", true);

    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId("test-client");

    X112_QttConnack connack = redirectHandler.handleConnect(connect);

    assertNotNull(connack);
    assertNotNull(connack.getServerReference());
    assertFalse(connack.getServerReference().equals("localhost:1883"));
  }

  @Test
  void testHandleConnectNoRedirect() {
    // 移除所有服务器
    for (String server : redirectService.getServers()) {
      redirectService.removeServer(server);
    }

    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId("test-client");

    X112_QttConnack connack = redirectHandler.handleConnect(connect);

    assertNull(connack);
  }

  @Test
  void testHandleMaintenanceConnect() {
    redirectService.addServer("backup:1883", 100);
    redirectService.setServerHealth("backup:1883", true);

    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId("test-client");

    X112_QttConnack connack = redirectHandler.handleMaintenanceConnect(connect);

    assertNotNull(connack);
  }

  @Test
  void testHandleMaintenanceConnectNoServer() {
    // 移除所有服务器
    for (String server : redirectService.getServers()) {
      redirectService.removeServer(server);
    }

    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId("test-client");

    X112_QttConnack connack = redirectHandler.handleMaintenanceConnect(connect);

    assertNotNull(connack);
    // 应该返回服务器不可用
    assertTrue(connack.isReject());
  }

  @Test
  void testCreateRedirectDisconnect() {
    X11E_QttDisconnect disconnect =
        redirectHandler.createRedirectDisconnect(RedirectReason.SERVER_MOVED, "newserver:1883");

    assertNotNull(disconnect);
    assertEquals(0x9D, disconnect.getCode());
    assertEquals("newserver:1883", disconnect.getServerReference());
    assertNotNull(disconnect.getReasonString());
    assertTrue(disconnect.isRedirect());
  }

  @Test
  void testRedirectCount() {
    String clientId = "test-client";

    assertEquals(0, redirectHandler.getRedirectCount(clientId));

    // 通过执行一次重定向来测试计数
    redirectService.addServer("testserver:1883", 100);
    redirectService.setServerHealth("testserver:1883", true);

    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId(clientId);

    // 处理 CONNECT 会记录重定向
    redirectHandler.handleConnect(connect);

    // 验证重定向计数已记录
    assertEquals(1, redirectHandler.getRedirectCount(clientId));

    redirectHandler.resetRedirectCount(clientId);
    assertEquals(0, redirectHandler.getRedirectCount(clientId));
  }

  @Test
  void testRedirectCooldown() throws InterruptedException {
    String clientId = "test-client";

    // 添加服务器
    redirectService.addServer("cooldown-server:1883", 100);
    redirectService.setServerHealth("cooldown-server:1883", true);

    // 第一次请求 - 会触发重定向
    X111_QttConnect connect1 = new X111_QttConnect();
    connect1.setClientId(clientId);
    X112_QttConnack connack1 = redirectHandler.handleConnect(connect1);
    assertNotNull(connack1);

    // 立即第二次请求应该被冷却
    X111_QttConnect connect2 = new X111_QttConnect();
    connect2.setClientId(clientId);
    X112_QttConnack connack2 = redirectHandler.handleConnect(connect2);
    assertNull(connack2); // 在冷却期内，不返回重定向
  }

  @Test
  void testSelectServerWithLoadBalancer() {
    loadBalancer.addServer("server1:1883", 100);
    loadBalancer.addServer("server2:1883", 100);

    String server = redirectHandler.selectServerWithLoadBalancer("client1");

    assertNotNull(server);
    assertTrue(server.equals("server1:1883") || server.equals("server2:1883"));
  }

  @Test
  void testUpdateLoadBalancerServers() {
    java.util.Map<String, Integer> servers = new java.util.HashMap<>();
    servers.put("new1:1883", 100);
    servers.put("new2:1883", 100);

    redirectHandler.updateLoadBalancerServers(servers);

    assertEquals(2, loadBalancer.getServers().size());
  }

  @Test
  void testSetLoadBalancerStrategy() {
    redirectHandler.setLoadBalancerStrategy(LoadBalancer.Strategy.LEAST_CONNECTIONS);
    assertEquals(LoadBalancer.Strategy.LEAST_CONNECTIONS, loadBalancer.getStrategy());
  }

  @Test
  void testRedirectHandlerStatus() {
    MqttRedirectHandler.RedirectHandlerStatus status = redirectHandler.getStatus();

    assertNotNull(status);
    assertTrue(status.isRedirectEnabled());
    assertNotNull(status.getLoadBalancerStats());
  }

  // ==================== 集成测试 ====================

  @Test
  void testFullRedirectFlow() {
    // 模拟客户端连接
    String clientId = "integration-client";
    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId(clientId);

    // 处理 CONNECT
    X112_QttConnack connack = redirectHandler.handleConnect(connect);

    // 验证重定向 CONNACK
    assertNotNull(connack);
    assertNotNull(connack.getServerReference());
    // 验证返回的是健康服务器之一
    assertTrue(
        redirectService.getServers().contains(connack.getServerReference()),
        "Server reference should be one of the configured servers");
    assertEquals(0x9C, connack.getReasonCode() & 0xFF);

    // 验证重定向计数已记录
    assertEquals(1, redirectHandler.getRedirectCount(clientId));
  }

  @Test
  void testServerMigrationFlow() {
    // 模拟服务器迁移
    java.util.Map<String, X11E_QttDisconnect> disconnects =
        redirectHandler.handleServerMigration("new-cluster:1883");

    assertNotNull(disconnects);
  }
}
