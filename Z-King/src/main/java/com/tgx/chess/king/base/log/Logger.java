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

package com.tgx.chess.king.base.log;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * @author William.d.zk
 */
public class Logger
        implements
        Serializable
{
    private static final long serialVersionUID = 6165161241382365689L;

    private final String           _Name;
    private final org.slf4j.Logger _Logger;

    private Logger(String name)
    {
        _Name = name;
        _Logger = LoggerFactory.getLogger(name);
    }

    public static Logger getLogger(String name)
    {
        return new Logger(name);
    }

    public void info(String msg)
    {
        _Logger.info(msg);
    }

    public void info(Object object)
    {
        _Logger.info(object.toString());
    }

    public void info(String formatter, Object... content)
    {
        _Logger.info(String.format(formatter, content));
    }

    public void warning(String msg)
    {
        _Logger.warn(msg);
    }

    public void warning(String msg, Throwable throwable)
    {
        _Logger.warn(msg, throwable);
    }

    public void warning(String formatter, Throwable throwable, Object... msg)
    {
        _Logger.warn(String.format(formatter, msg), throwable);
    }

    public void warning(String formatter, Object... msg)
    {
        _Logger.warn(String.format(formatter, msg));
    }

    public void debug(Object object)
    {
        _Logger.debug(object.toString());
    }

    public void debug(String msg)
    {
        _Logger.debug(msg);
    }

    public void debug(String formatter, Object... content)
    {
        _Logger.debug(String.format(formatter, content));
    }

    public void debug(String msg, Throwable throwable)
    {
        _Logger.debug(msg, throwable);
    }

    public void fetal(String msg, Throwable throwable)
    {
        _Logger.error(msg, throwable);
    }

    public void fetal(String msg)
    {
        _Logger.error(msg);
    }

    public void fetal(String formatter, Object... params)
    {
        _Logger.error(formatter, params);
    }

    public boolean isEnable(Level level)
    {
        switch (level)
        {
            case INFO:
                return _Logger.isInfoEnabled();
            case DEBUG:
                return _Logger.isDebugEnabled();
            case WARN:
                return _Logger.isWarnEnabled();
            case ERROR:
                return _Logger.isErrorEnabled();
            case TRACE:
                return _Logger.isTraceEnabled();
            default:
                return false;
        }
    }

    public static String arrayToString(Object[] a)
    {
        if (Objects.isNull(a)) { return "[]"; }
        int iMax = a.length - 1;
        if (iMax == -1) { return "[]"; }

        StringBuilder b = new StringBuilder();
        b.append("[\n");
        for (int i = 0;; i++) {
            b.append("\t\t")
             .append(a[i]);
            if (i == iMax) { return b.append("\n]")
                                     .toString(); }
            b.append(",\n");
        }
    }

    /**
     * 判断当前行是否存在有效数据，不存在返回 true 跳过当前行
     *
     * @param line
     *            读入的当前行
     * @return 是否跳过
     */
    public static boolean skipLine(String line)
    {
        return Objects.isNull(line) || "".equals(line) || line.matches("\\s+");
    }
}