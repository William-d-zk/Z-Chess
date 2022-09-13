/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.resource.db.config;

import com.isahl.chess.rook.storage.db.service.RookProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "com.isahl.chess.pawn.endpoint.device.api.db.repository",
                       entityManagerFactoryRef = "api-entity-manager-factory",
                       transactionManagerRef = "api-transaction-manager")
public class ApiJpaConfig
{
    private final RookProvider _RookProvider;

    @Autowired
    public ApiJpaConfig(RookProvider rookProvider) {_RookProvider = rookProvider;}

    @Bean("api-entity-manager-factory")
    public LocalContainerEntityManagerFactoryBean createRemoteEntityManagerFactory()
    {
        return _RookProvider.buildEntityManager("0",
                                                "com.isahl.chess.pawn.endpoint.device.db.central.model",
                                                "com.isahl.chess.pawn.endpoint.device.api.db.model");
    }

    @Bean("api-transaction-manager")
    public PlatformTransactionManager createRemoteTransactionManager(
            @Qualifier("api-entity-manager-factory")
            LocalContainerEntityManagerFactoryBean factory)
    {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(factory.getObject());
        return tm;
    }
}
