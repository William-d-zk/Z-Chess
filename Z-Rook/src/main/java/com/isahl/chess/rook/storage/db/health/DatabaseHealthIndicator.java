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

import com.isahl.chess.king.base.log.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库健康检查组件
 * 
 * 在应用启动时检查数据库连接，如果连接失败则自动退出应用。
 * 确保应用不会在无数据库环境下运行。
 * 
 * @author Z-Chess Security Team
 * @since 1.0.20
 */
@Component
@Order(1) // 确保最先执行
public class DatabaseHealthIndicator implements ApplicationRunner {

    private static final Logger _Logger = Logger.getLogger(DatabaseHealthIndicator.class.getSimpleName());

    private final DataSource _DataSource;
    private final ApplicationContext _ApplicationContext;

    @Autowired
    public DatabaseHealthIndicator(DataSource dataSource, ApplicationContext applicationContext) {
        _DataSource = dataSource;
        _ApplicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        _Logger.info("正在检查数据库连接...");
        
        if (_DataSource == null) {
            _Logger.fetal("✗ 数据库连接失败: DataSource 未配置");
            _Logger.fetal("  请检查以下配置:");
            _Logger.fetal("  1. 确保设置了 POSTGRES_PASSWORD 环境变量");
            _Logger.fetal("  2. 确保 PostgreSQL 服务已启动");
            _Logger.fetal("  3. 检查 spring.datasource.url 配置");
            _Logger.fetal("  4. 如果使用本地环境，请运行: docker run -d -e POSTGRES_PASSWORD=your_password -p 5432:5432 postgres:17");
            _Logger.fetal("应用将在 5 秒后退出...");
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            SpringApplication.exit(_ApplicationContext, () -> 1);
            System.exit(1);
        }

        try (Connection connection = _DataSource.getConnection()) {
            if (connection != null && connection.isValid(5)) {
                String databaseUrl = connection.getMetaData().getURL();
                String databaseProduct = connection.getMetaData().getDatabaseProductName();
                _Logger.info("✓ 数据库连接成功 [%s] - %s", databaseProduct, databaseUrl);
            } else {
                throw new SQLException("数据库连接无效");
            }
        } catch (SQLException e) {
            _Logger.fetal("✗ 数据库连接失败: %s", e.getMessage());
            _Logger.fetal("  请检查以下配置:");
            _Logger.fetal("  1. 确保设置了 POSTGRES_PASSWORD 环境变量");
            _Logger.fetal("     export POSTGRES_PASSWORD=your_secure_password");
            _Logger.fetal("  2. 确保 PostgreSQL 服务已启动");
            _Logger.fetal("     docker ps | grep postgres");
            _Logger.fetal("  3. 检查数据库连接配置");
            _Logger.fetal("     spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}");
            _Logger.fetal("  4. 检查网络连接");
            _Logger.fetal("     telnet ${POSTGRES_HOST} ${POSTGRES_PORT}");
            _Logger.fetal("应用将在 5 秒后退出...");
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            SpringApplication.exit(_ApplicationContext, () -> 1);
            System.exit(1);
        }
    }
}
