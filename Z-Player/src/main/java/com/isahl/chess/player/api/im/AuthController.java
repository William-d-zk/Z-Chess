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

package com.isahl.chess.player.api.im;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.player.domain.User;
import com.isahl.chess.player.domain.UserSession;
import com.isahl.chess.player.repository.UserRepository;
import com.isahl.chess.player.repository.UserSessionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/im/auth")
public class AuthController {
  private static final Logger _Logger = LoggerFactory.getLogger(AuthController.class);

  private final UserRepository _UserRepository;
  private final UserSessionRepository _SessionRepository;

  @Autowired
  public AuthController(UserRepository userRepository, UserSessionRepository sessionRepository) {
    _UserRepository = userRepository;
    _SessionRepository = sessionRepository;
  }

  @PostMapping("login")
  public ZResponse<?> login(@RequestBody LoginRequest request) {
    Optional<User> userOpt = _UserRepository.findByUsername(request.username);
    if (userOpt.isEmpty()) {
      return ZResponse.error("User not found");
    }
    User user = userOpt.get();
    if (!user.getPasswordHash().equals(request.password)) {
      return ZResponse.error("Invalid password");
    }
    String token = UUID.randomUUID().toString();
    UserSession session =
        new UserSession(user.getId(), token, request.clientType, request.clientIp);
    _SessionRepository.save(session);
    user.setOnline(true);
    _UserRepository.save(user);
    _Logger.info("User logged in: username={}, token={}", request.username, token);
    return ZResponse.success(
        Map.of(
            "token", token,
            "userId", user.getId(),
            "username", user.getUsername(),
            "displayName", user.getDisplayName()));
  }

  @PostMapping("logout")
  public ZResponse<?> logout(@RequestHeader("X-Session-Token") String token) {
    Optional<UserSession> sessionOpt = _SessionRepository.findBySessionToken(token);
    if (sessionOpt.isEmpty()) {
      return ZResponse.error("Invalid session");
    }
    UserSession session = sessionOpt.get();
    session.setActive(false);
    session.setLogoutTime(System.currentTimeMillis());
    _SessionRepository.save(session);
    _UserRepository.findById(session.getUserId())
        .ifPresent(
            user -> {
              user.setOnline(false);
              _UserRepository.save(user);
            });
    _Logger.info("User logged out: token={}", token);
    return ZResponse.success("Logged out successfully");
  }

  @GetMapping("verify")
  public ZResponse<?> verify(@RequestHeader("X-Session-Token") String token) {
    Optional<UserSession> sessionOpt = _SessionRepository.findBySessionToken(token);
    if (sessionOpt.isEmpty() || !sessionOpt.get().getActive()) {
      return ZResponse.error("Invalid or expired session");
    }
    UserSession session = sessionOpt.get();
    Optional<User> userOpt = _UserRepository.findById(session.getUserId());
    if (userOpt.isEmpty()) {
      return ZResponse.error("User not found");
    }
    User user = userOpt.get();
    return ZResponse.success(
        Map.of(
            "userId", user.getId(),
            "username", user.getUsername(),
            "displayName", user.getDisplayName()));
  }

  public static class LoginRequest {
    public String username;
    public String password;
    public String clientType;
    public String clientIp;
  }
}
