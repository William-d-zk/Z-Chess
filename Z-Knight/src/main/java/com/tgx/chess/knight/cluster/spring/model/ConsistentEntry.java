/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.knight.cluster.spring.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * @author william.d.zk
 * @date 2020/4/23
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ConsistentEntry
{
    private long               session;
    private long               zuid;
    private boolean            commit;
    private ConsistentProtocol protocol;

    public boolean isCommit()
    {
        return commit;
    }

    public void setCommit(boolean commit)
    {
        this.commit = commit;
    }

    public long getSession()
    {
        return session;
    }

    public void setSession(long session)
    {
        this.session = session;
    }

    public long getZuid()
    {
        return zuid;
    }

    public void setZuid(long zuid)
    {
        this.zuid = zuid;
    }

    public ConsistentProtocol getProtocol()
    {
        return protocol;
    }

    public void setProtocol(ConsistentProtocol protocol)
    {
        this.protocol = protocol;
    }
}
