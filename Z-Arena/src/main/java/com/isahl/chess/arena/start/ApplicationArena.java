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

package com.isahl.chess.arena.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Z-Chess Arena 网关应用启动类
 *
 * <p>安全更新 (v1.0.20): - 移除了 H2 数据库支持 - 添加了数据库健康检查，无数据库时自动退出 - 强制要求配置 PostgreSQL
 *
 * @author william.d.zk
 * @date 2021/02/17
 */
@EnableScheduling
@Configuration
@ComponentScan(
    basePackages = {
      "com.isahl.chess.knight.raft",
      "com.isahl.chess.knight.cluster",
      "com.isahl.chess.rook.storage",
      "com.isahl.chess.pawn.endpoint",
      "com.isahl.chess.player.api",
      "com.isahl.chess.arena.gateway"
    })
@EnableAutoConfiguration
public class ApplicationArena {

  public static void main(String[] args) {
    checkRequiredEnvironment();
    SpringApplication.run(ApplicationArena.class, args);
  }

  /** 检查必需的环境变量 在无数据库环境下给出明确提示 */
  private static void checkRequiredEnvironment() {
    String postgresPassword = System.getenv("POSTGRES_PASSWORD");
    if (postgresPassword == null || postgresPassword.isEmpty()) {
      throw new IllegalStateException(
          "Missing required environment variable POSTGRES_PASSWORD. "
              + "H2 has been removed; PostgreSQL is mandatory. "
              + "Steps: 1) export POSTGRES_PASSWORD=your_secure_password "
              + "2) docker run -d -e POSTGRES_PASSWORD=your_password -e POSTGRES_USER=chess "
              + "-e POSTGRES_DB=zchess_test -p 5432:5432 postgres:17 "
              + "3) or use an existing PostgreSQL instance.");
    }
  }
}
