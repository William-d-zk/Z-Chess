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

package com.isahl.chess.referee.security.jpa.config;

import com.isahl.chess.rook.storage.jpa.config.BaseJpaConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * @author william.d.zk
 * @date 2021/2/5
 */
@Configuration("security_jpa_config")
@EnableJpaRepositories(basePackages = { "com.isahl.chess.referee.security.jpa.repository" },
                       entityManagerFactoryRef = "security-entity-manager",
                       transactionManagerRef = "security-transaction-manager")
public class SecurityJpaConfig
        extends BaseJpaConfig
{
    @Bean("security-entity-manager")
    public LocalContainerEntityManagerFactoryBean createSecurityEntityManager(
            @Qualifier("primary-data-source")
                    DataSource dataSource,
            @Qualifier("primary-jpa-properties")
                    JpaProperties jpaProperties,
            @Qualifier("primary-jpa-hibernate-properties")
                    HibernateProperties hibernateProperties,
            @Qualifier("primary-sql-init-settings")
                    DatabaseInitializationSettings initializationSettings)
    {
        return getEntityManager(dataSource,
                                jpaProperties,
                                hibernateProperties,
                                initializationSettings,
                                "com.isahl.chess.referee.security.jpa.model");
    }

    @Bean("security-transaction-manager")
    public PlatformTransactionManager createSecurityTransactionManager(
            @Qualifier("security-entity-manager")
                    LocalContainerEntityManagerFactoryBean factory)
    {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(factory.getObject());
        return tm;
    }
}
