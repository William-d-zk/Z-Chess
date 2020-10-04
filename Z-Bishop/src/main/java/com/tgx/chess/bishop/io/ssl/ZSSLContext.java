/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.bishop.io.ssl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 */
public class ZSSLContext
        extends
        ZContext
{
    private final static Logger LOGGER = Logger.getLogger(ZSSLContext.class.getSimpleName());

    public ZSSLContext(ISessionOption option, ISort<ZContext> sort) throws NoSuchAlgorithmException
    {
        super(option, sort);
        _SslContext = SSLContext.getInstance("TLSv1.3");
        _SslEngine = _SslContext.createSSLEngine();

    }

    private final SSLEngine  _SslEngine;
    private final SSLContext _SslContext;

    public SSLEngine getSSLEngine()
    {
        return _SslEngine;
    }

    public SSLContext getSSLContext()
    {
        return _SslContext;
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        try
        {
            _SslEngine.closeInbound();
        }
        catch (SSLException e)
        {
            LOGGER.info("ssl in bound exception %s", e.getMessage());
        }
        _SslEngine.closeOutbound();
    }
}
