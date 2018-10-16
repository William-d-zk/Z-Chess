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
package com.tgx.chess.queen.event.inf;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public interface IError
{

    int getCode();

    String getMsg(Object... args);

    enum Type
            implements
            IError
    {

        CONNECT_FAILED(300, " 连接失败 "),
        ACCEPT_FAILED(301, " 应答连接失败 "),
        READ_ZERO(401, " 0Byte,缓存区触发读,无数据 "),
        READ_EOF(402, " 读到流终止EOF "),
        READ_FAILED(403, " 读取缓存失败 "),
        WRITE_ZERO(501, " 写入0Byte "),
        WRITE_EOF(502, " 写入出现EOF "),
        WRITE_FAILED(503, " 写入缓存失败 "),
        CLOSED(601, " 链路管理"),
        SSL_HANDSHAKE(701, " SSL握手失败 "),
        FILTER_ENCODE(801, " 装箱失败 "),
        FILTER_DECODE(802, " 拆箱失败 "),
        OUT_OF_LENGTH(803, " 数据越界 "),
        ILLEGAL_STATE(901, " 逻辑状态错误 "),
        ILLEGAL_BIZ_STATE(902, " 业务状态错误 "),
        TIME_OUT(101, " 超时 "),
        NO_ERROR(200, " 成功 "),;

        private final int    _Code;
        private final String _MsgFormatter;

        Type(int code, String formatter) {
            _Code = code;
            _MsgFormatter = formatter;
        }

        @Override
        public int getCode() {
            return _Code;
        }

        @Override
        public String getMsg(Object... args) {
            return Objects.isNull(args) || args.length == 0 ? _MsgFormatter : String.format(_MsgFormatter, args);
        }

    }
}