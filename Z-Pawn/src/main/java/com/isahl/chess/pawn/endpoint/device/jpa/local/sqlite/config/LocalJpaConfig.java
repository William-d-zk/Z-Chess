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

package com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.config;

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

@Configuration
@EnableJpaRepositories(basePackages = "com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.repository",
                       entityManagerFactoryRef = "local-entity-manager",
                       transactionManagerRef = "local-transaction-manager")
public class LocalJpaConfig
        extends BaseJpaConfig
{
    @Bean("local-entity-manager")
    public LocalContainerEntityManagerFactoryBean createLocalEntityManager(
            @Qualifier("secondary-data-source")
                    DataSource dataSource,
            @Qualifier("secondary-jpa-properties")
                    JpaProperties jpaProperties,
            @Qualifier("secondary-jpa-hibernate-properties")
                    HibernateProperties hibernateProperties,
            @Qualifier("secondary-sql-init-settings")
                    DatabaseInitializationSettings initializationSettings

                                                                          )
    {
        return getEntityManager(dataSource,
                                jpaProperties,
                                hibernateProperties,
                                initializationSettings,
                                "com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.model");
    }

    @Bean("local-transaction-manager")
    public PlatformTransactionManager createLocalTransactionManager(
            @Qualifier("local-entity-manager")
                    LocalContainerEntityManagerFactoryBean factory)
    {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(factory.getObject());
        return tm;
    }
}
