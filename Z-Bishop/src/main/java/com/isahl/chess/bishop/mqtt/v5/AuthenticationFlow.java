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

package com.isahl.chess.bishop.mqtt.v5;

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider.AuthContext;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider.AuthResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT v5.0 增强认证流程管理器
 *
 * <p>管理多步认证流程，包括：
 *
 * <ul>
 *   <li>认证状态跟踪
 *   <li>超时处理
 *   <li>认证提供者路由
 *   <li>重新认证管理
 * </ul>
 *
 * @author william.d.zk
 * @since 1.2.0
 */
public class AuthenticationFlow {

  private static final Logger _Logger =
      LoggerFactory.getLogger("mqtt.auth." + AuthenticationFlow.class.getSimpleName());

  /** 默认认证超时时间（秒） */
  public static final int DEFAULT_AUTH_TIMEOUT_SECONDS = 60;

  /** 认证会话映射 */
  private final Map<String, AuthSession> _sessions = new ConcurrentHashMap<>();

  /** 认证提供者映射 */
  private final Map<String, IQttAuthProvider> _providers = new ConcurrentHashMap<>();

  /** 超时清理调度器 */
  private final ScheduledExecutorService _scheduler;

  /** 认证超时时间（秒） */
  private final int _authTimeoutSeconds;

  public AuthenticationFlow() {
    this(DEFAULT_AUTH_TIMEOUT_SECONDS);
  }

  public AuthenticationFlow(int authTimeoutSeconds) {
    _authTimeoutSeconds = authTimeoutSeconds;
    _scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "auth-timeout-cleaner");
              t.setDaemon(true);
              return t;
            });
    startTimeoutCleaner();
  }

  // ==================== 提供者管理 ====================

  /**
   * 注册认证提供者
   *
   * @param provider 认证提供者
   */
  public void registerProvider(IQttAuthProvider provider) {
    String method = provider.getAuthMethod();
    _providers.put(method, provider);
    _Logger.info("Registered auth provider: {}", method);
  }

  /** 注销认证提供者 */
  public void unregisterProvider(String authMethod) {
    _providers.remove(authMethod);
    _Logger.info("Unregistered auth provider: {}", authMethod);
  }

  /** 获取认证提供者 */
  public IQttAuthProvider getProvider(String authMethod) {
    return _providers.get(authMethod);
  }

  /** 检查是否有指定认证方法的提供者 */
  public boolean hasProvider(String authMethod) {
    return _providers.containsKey(authMethod);
  }

  // ==================== 认证流程 ====================

  /**
   * 开始认证流程
   *
   * @param clientId 客户端 ID
   * @param authMethod 认证方法
   * @param authData 初始认证数据
   * @return 认证结果
   */
  public AuthResult startAuth(String clientId, String authMethod, byte[] authData) {
    IQttAuthProvider provider = _providers.get(authMethod);
    if (provider == null) {
      _Logger.warn("No auth provider found for method: {}", authMethod);
      return AuthResult.failure("Unsupported authentication method: " + authMethod);
    }

    // 创建认证会话
    AuthSession session = new AuthSession(clientId, authMethod, provider);
    _sessions.put(clientId, session);

    // 创建认证上下文
    AuthContext context = new AuthContext(System.currentTimeMillis(), clientId, authMethod);
    session.setContext(context);

    // 执行初始认证
    // 构造基础连接对象，避免向 provider 传入 null 导致 NPE
    X111_QttConnect connect = new X111_QttConnect();
    connect.setClientId(clientId);
    if (authData != null && authData.length > 0) {
      connect.setAuthenticationData(authData);
    }
    AuthResult result = provider.startAuth(connect);

    if (result.isSuccess()) {
      session.setState(AuthState.SUCCESS);
      _sessions.remove(clientId);
      _Logger.info("Authentication succeeded immediately for client: {}", clientId);
    } else if (result.isContinue()) {
      session.setState(AuthState.IN_PROGRESS);
      _Logger.debug("Authentication continuing for client: {}", clientId);
    } else {
      session.setState(AuthState.FAILED);
      _sessions.remove(clientId);
      _Logger.warn("Authentication failed for client: {} - {}", clientId, result.getReason());
    }

    return result;
  }

  /**
   * 继续认证流程
   *
   * @param clientId 客户端 ID
   * @param authData 认证数据
   * @return 认证结果
   */
  public AuthResult continueAuth(String clientId, byte[] authData) {
    AuthSession session = _sessions.get(clientId);
    if (session == null) {
      _Logger.warn("No active auth session for client: {}", clientId);
      return AuthResult.failure("No active authentication session");
    }

    if (session.getState() != AuthState.IN_PROGRESS) {
      _Logger.warn("Invalid auth state for client: {} - {}", clientId, session.getState());
      return AuthResult.failure("Invalid authentication state");
    }

    IQttAuthProvider provider = session.getProvider();
    AuthContext context = session.getContext();
    context.nextStep();

    AuthResult result = provider.continueAuth(session.getAuthMethod(), authData, context);

    if (result.isSuccess()) {
      session.setState(AuthState.SUCCESS);
      _sessions.remove(clientId);
      _Logger.info("Authentication succeeded for client: {}", clientId);
    } else if (result.isContinue()) {
      session.updateLastActivity();
      _Logger.debug(
          "Authentication continuing for client: {} (step {})", clientId, context.getStep());
    } else {
      session.setState(AuthState.FAILED);
      _sessions.remove(clientId);
      _Logger.warn("Authentication failed for client: {} - {}", clientId, result.getReason());
    }

    return result;
  }

  /**
   * 重新认证
   *
   * @param clientId 客户端 ID
   * @param authMethod 认证方法
   * @param authData 认证数据
   * @param sessionId 会话 ID
   * @return 认证结果
   */
  public AuthResult reauth(String clientId, String authMethod, byte[] authData, long sessionId) {
    IQttAuthProvider provider = _providers.get(authMethod);
    if (provider == null) {
      return AuthResult.failure("Unsupported authentication method");
    }

    _Logger.info("Re-authentication request from client: {} for session: {}", clientId, sessionId);

    return provider.reauth(authMethod, authData, sessionId);
  }

  /** 取消认证流程 */
  public void cancelAuth(String clientId) {
    AuthSession session = _sessions.remove(clientId);
    if (session != null) {
      session.setState(AuthState.CANCELLED);
      _Logger.info("Authentication cancelled for client: {}", clientId);
    }
  }

  /** 获取认证会话状态 */
  public AuthState getAuthState(String clientId) {
    AuthSession session = _sessions.get(clientId);
    return session != null ? session.getState() : AuthState.NONE;
  }

  // ==================== 超时清理 ====================

  private void startTimeoutCleaner() {
    _scheduler.scheduleWithFixedDelay(
        this::cleanExpiredSessions, _authTimeoutSeconds, _authTimeoutSeconds, TimeUnit.SECONDS);
  }

  private void cleanExpiredSessions() {
    long now = System.currentTimeMillis();
    long timeoutMillis = _authTimeoutSeconds * 1000L;

    _sessions
        .entrySet()
        .removeIf(
            entry -> {
              AuthSession session = entry.getValue();
              if (now - session.getLastActivity() > timeoutMillis) {
                session.setState(AuthState.TIMEOUT);
                _Logger.warn("Authentication timeout for client: {}", entry.getKey());
                return true;
              }
              return false;
            });
  }

  // ==================== 统计信息 ====================

  /** 获取活跃认证会话数 */
  public int getActiveSessionCount() {
    return (int)
        _sessions.values().stream().filter(s -> s.getState() == AuthState.IN_PROGRESS).count();
  }

  /** 获取总认证会话数 */
  public int getTotalSessionCount() {
    return _sessions.size();
  }

  /** 关闭认证流程管理器 */
  public void shutdown() {
    _scheduler.shutdown();
    _sessions.clear();
    _Logger.info("Authentication flow manager shutdown");
  }

  // ==================== 内部类 ====================

  /** 认证状态 */
  public enum AuthState {
    /** 无认证 */
    NONE,
    /** 认证进行中 */
    IN_PROGRESS,
    /** 认证成功 */
    SUCCESS,
    /** 认证失败 */
    FAILED,
    /** 认证超时 */
    TIMEOUT,
    /** 认证取消 */
    CANCELLED
  }

  /** 认证会话 */
  private static class AuthSession {
    private final String _clientId;
    private final String _authMethod;
    private final IQttAuthProvider _provider;
    private AuthState _state;
    private AuthContext _context;
    private long _lastActivity;

    AuthSession(String clientId, String authMethod, IQttAuthProvider provider) {
      _clientId = clientId;
      _authMethod = authMethod;
      _provider = provider;
      _state = AuthState.IN_PROGRESS;
      _lastActivity = System.currentTimeMillis();
    }

    String getClientId() {
      return _clientId;
    }

    String getAuthMethod() {
      return _authMethod;
    }

    IQttAuthProvider getProvider() {
      return _provider;
    }

    AuthState getState() {
      return _state;
    }

    void setState(AuthState state) {
      _state = state;
    }

    AuthContext getContext() {
      return _context;
    }

    void setContext(AuthContext context) {
      _context = context;
    }

    long getLastActivity() {
      return _lastActivity;
    }

    void updateLastActivity() {
      _lastActivity = System.currentTimeMillis();
    }
  }
}
