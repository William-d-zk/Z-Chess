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

package com.isahl.chess.rook.storage.jpa.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@EnableJpaAuditing
@EnableTransactionManagement
@Configuration("base_jpa_config")
@ConditionalOnMissingBean(name = { "jpaAuditingHandler" })
@PropertySource({ "classpath:db.properties" })
public class BaseJpaConfig
{
    @Bean("primary-data-source")
    @ConfigurationProperties(prefix = "z.rook.primary.datasource")
    public DataSource getPrimaryDataSource()
    {
        return DataSourceBuilder.create()
                                .build();
    }

    @Bean("secondary-data-source")
    @ConfigurationProperties(prefix = "z.rook.secondary.datasource")
    public DataSource getSecondaryDataSource()
    {
        return DataSourceBuilder.create()
                                .build();
    }

    @Bean("primary-jpa-properties")
    @ConfigurationProperties(prefix = "z.rook.primary.jpa")
    public JpaProperties primaryJpaProperties()
    {
        return new JpaProperties();
    }

    @Bean("secondary-jpa-properties")
    @ConfigurationProperties(prefix = "z.rook.secondary.jpa")
    public JpaProperties secondaryJpaProperties()
    {
        return new JpaProperties();
    }

    protected LocalContainerEntityManagerFactoryBean getEntityManager(DataSource dataSource,
                                                                      JpaProperties jpaProperties,
                                                                      String... packagesToScan)
    {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setShowSql(jpaProperties.isShowSql());
        jpaVendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        jpaVendorAdapter.setDatabase(jpaProperties.getDatabase());
        em.setJpaVendorAdapter(jpaVendorAdapter);
        em.setJpaPropertyMap(jpaProperties.getProperties());
        em.setPackagesToScan(packagesToScan);
        return em;
    }

    @Bean("primary-sql-init-settings")
    @ConfigurationProperties("z.rook.primary.sql.init")
    public DatabaseInitializationSettings getPrimarySqlInitializationSettings()
    {
        return new DatabaseInitializationSettings();
    }

    @Bean("secondary-sql-init-settings")
    @ConfigurationProperties("z.rook.secondary.sql.init")
    public DatabaseInitializationSettings getSecondarySqlInitializationSettings()
    {
        return new DatabaseInitializationSettings();
    }

    @Bean("primary-sql-initializer")
    public DataSourceScriptDatabaseInitializer getPrimarySqlInitializer(
            @Qualifier("primary-data-source")
                    DataSource dataSource,
            @Qualifier("primary-sql-init-settings")
                    DatabaseInitializationSettings settings)
    {
        return new DataSourceScriptDatabaseInitializer(dataSource, settings);
    }

    @Bean("secondary-sql-initializer")
    public DataSourceScriptDatabaseInitializer getSecondarySqlInitializer(
            @Qualifier("secondary-data-source")
                    DataSource dataSource,
            @Qualifier("secondary-sql-init-settings")
                    DatabaseInitializationSettings settings)
    {
        return new DataSourceScriptDatabaseInitializer(dataSource, settings);
    }
}
