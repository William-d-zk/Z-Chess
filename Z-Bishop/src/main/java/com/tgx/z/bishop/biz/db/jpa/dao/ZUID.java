/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.z.bishop.biz.db.jpa.dao;

import java.io.Serializable;
import java.time.Instant;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * 00-0000-000-0000000-0000000000000000000000000000000000000-000000000
 * -2bit-
 * 10 Client manager service
 * 11 Cluster symmetry communication
 * 01 Internal message queue broker
 * 00 Device consumer connection
 * -4bit-
 * Cluster region
 * -3bit-
 * Cluster set identity
 * -7bit-
 * Endpoint identity
 * -38bit-
 * Timestamp gap 2019-01-01 00:00:00.000
 * -10bit-
 * sequence in one millisecond
 */

public class ZUID
        implements
        IdentifierGenerator
{
    private static final long TWEPOCH = Instant.parse("2019-01-01T00:00:00.00Z")
                                               .toEpochMilli();



    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return null;
    }
}
