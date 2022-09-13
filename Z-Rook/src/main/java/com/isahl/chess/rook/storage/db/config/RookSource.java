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

package com.isahl.chess.rook.storage.db.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;

import java.util.List;

public class RookSource
{
    private List<ds> dss;

    public List<ds> getDss()
    {
        return dss;
    }

    public void setDss(List<ds> dss)
    {
        this.dss = dss;
    }

    public static class ds
    {

        JpaProperties               jpa;
        DataSourceProperties        datasource;
        SqlInitializationProperties sqlInit;

        public JpaProperties getJpa()
        {
            return jpa;
        }

        public void setJpa(JpaProperties jpa)
        {
            this.jpa = jpa;
        }

        public DataSourceProperties getDatasource()
        {
            return datasource;
        }

        public void setDatasource(DataSourceProperties datasource)
        {
            this.datasource = datasource;
        }

        public SqlInitializationProperties getSqlInit()
        {
            return sqlInit;
        }

        public void setSqlInit(SqlInitializationProperties sqlInit)
        {
            this.sqlInit = sqlInit;
        }
    }

}
