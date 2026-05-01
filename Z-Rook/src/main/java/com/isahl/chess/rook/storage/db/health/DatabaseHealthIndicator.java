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

package com.isahl.chess.rook.storage.db.health;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据库健康检查组件
 *
 * <p>在应用启动时检查数据库连接，如果连接失败则自动退出应用。 确保应用不会在无数据库环境下运行。
 *
 * @author Z-Chess Security Team
 * @since 1.0.20
 */
@Component
@Order(1) // 确保最先执行
public class DatabaseHealthIndicator implements ApplicationRunner {

  private static final Logger _Logger =
      LoggerFactory.getLogger(DatabaseHealthIndicator.class.getSimpleName());

  private final DataSource _DataSource;

  @Autowired
  public DatabaseHealthIndicator(DataSource dataSource) {
    _DataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    _Logger.info("正在检查数据库连接...");

    if (_DataSource == null) {
      throw new IllegalStateException(
          "Database connection failed: DataSource not configured. "
              + "Please ensure: 1) POSTGRES_PASSWORD is set, "
              + "2) PostgreSQL is running, "
              + "3) spring.datasource.url is correct. "
              + "Local setup: docker run -d -e POSTGRES_PASSWORD=your_password -p 5432:5432 postgres:17");
    }

    try (Connection connection = _DataSource.getConnection()) {
      if (connection != null && connection.isValid(5)) {
        String databaseUrl = connection.getMetaData().getURL();
        String databaseProduct = connection.getMetaData().getDatabaseProductName();
        _Logger.info("数据库连接成功 [%s] - %s", databaseProduct, databaseUrl);
      } else {
        throw new SQLException("数据库连接无效");
      }
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Database connection failed: "
              + e.getMessage()
              + ". Please ensure: 1) POSTGRES_PASSWORD is set, "
              + "2) PostgreSQL is running, "
              + "3) spring.datasource.url is correct, "
              + "4) Network connectivity is available.",
          e);
    }
  }
}
