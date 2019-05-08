/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.bishop.io.zprotocol.ztls;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.BaseCommand;

/**
 * @author William.d.zk
 */
public class X06_PlainStart<C extends WsContext>
        extends
        BaseCommand<C>
{
    public final static int COMMAND = 0x06;
    public int              code;

    public X06_PlainStart()
    {
        super(COMMAND, false);
    }

    @Override
    public boolean isMappingCommand()
    {
        return true;
    }

    public X06_PlainStart(int code)
    {
        super(COMMAND, false);
        this.code = code;
    }

    @Override
    public int getPriority()
    {
        return QOS_00_NETWORK_CONTROL;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        code = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(code, data, pos);
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 2;
    }
}
