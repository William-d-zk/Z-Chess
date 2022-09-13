/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.rook.storage.db.service;

import com.isahl.chess.rook.storage.db.config.RookSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Service
public class RookProvider
{
    private final Map<String, RookSource.ds> _SourceMapping;

    @Autowired
    public RookProvider(RookSource source)
    {
        if(source.getDss() != null && !source.getDss()
                                             .isEmpty())
        {
            _SourceMapping = new HashMap<>();
            source.getDss()
                  .forEach(ds->{
                      String name = ds.getDatasource()
                                      .getName();
                      _SourceMapping.putIfAbsent(name, ds);
                  });
        }
        else {
            _SourceMapping = null;
        }
    }

    public LocalContainerEntityManagerFactoryBean buildEntityManager(String source, String... packagesToScan)
    {
        RookSource.ds ds = _SourceMapping.get(source);
        if(ds != null) {
            DataSourceProperties dp = ds.getDatasource();
            DataSource dataSource = dp.initializeDataSourceBuilder()
                                      .build();
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            JpaProperties jpaProperties = ds.getJpa();
            HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
            jpaVendorAdapter.setShowSql(jpaProperties.isShowSql());
            jpaVendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
            jpaVendorAdapter.setDatabase(jpaProperties.getDatabase());
            emf.setJpaVendorAdapter(jpaVendorAdapter);
            emf.setJpaPropertyMap(new HibernateProperties().determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings()));
            emf.setPackagesToScan(packagesToScan);
            new SqlDataSourceScriptDatabaseInitializer(dataSource, ds.getSqlInit()).initializeDatabase();
            return emf;
        }
        return null;
    }

}
