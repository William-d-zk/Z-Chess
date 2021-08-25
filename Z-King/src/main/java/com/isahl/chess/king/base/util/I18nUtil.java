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
package com.isahl.chess.king.base.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author William.d.zk
 */
public interface I18nUtil
{
    int CHARSET_ASCII       = 0x00;
    int CHARSET_UTF_8       = 0x01 << 4;
    int CHARSET_UTF_8_NB    = 0x02 << 4;
    int CHARSET_UTC_BE      = 0x03 << 4;
    int CHARSET_UTC_LE      = 0x04 << 4;
    int CHARSET_GBK         = 0x05 << 4;
    int CHARSET_GB2312      = 0x06 << 4;
    int CHARSET_GB18030     = 0x07 << 4;
    int CHARSET_ISO_8859_1  = 0x08 << 4;
    int CHARSET_ISO_8859_15 = 0x09 << 4;
    int SERIAL_TEXT         = 0x01;
    int SERIAL_BINARY       = 0x02;
    int SERIAL_JSON         = 0x03;
    int SERIAL_XML          = 0x04;
    int SERIAL_PROXY        = 0x05;

    static Charset getCharset(byte data)
    {
        return switch (data & 0xF0)
        {
            case CHARSET_ASCII -> StandardCharsets.US_ASCII;
            case CHARSET_UTF_8_NB -> StandardCharsets.UTF_16;
            case CHARSET_UTC_BE -> StandardCharsets.UTF_16BE;
            case CHARSET_UTC_LE -> StandardCharsets.UTF_16LE;
            case CHARSET_GBK -> Charset.forName("GBK");
            case CHARSET_GB2312 -> Charset.forName("GB2312");
            case CHARSET_GB18030 -> Charset.forName("GB18030");
            case CHARSET_ISO_8859_1 -> StandardCharsets.ISO_8859_1;
            default -> StandardCharsets.UTF_8;
        };
    }

    static int getCharsetCode(String charset)
    {
        if (charset.equals("ASCII")) return CHARSET_ASCII;
        if (charset.equals("UTF-8")) return CHARSET_UTF_8;
        if (charset.equals("UTF-16")) return CHARSET_UTF_8_NB;
        if (charset.equals("UTF-16BE")) return CHARSET_UTC_BE;
        if (charset.equals("UTF-16LE")) return CHARSET_UTC_LE;
        if (charset.equals("GBK")) return CHARSET_GBK;
        if (charset.equals("GB2312")) return CHARSET_GB2312;
        if (charset.equals("GB18030")) return CHARSET_GB18030;
        if (charset.equals("ISO-8859-1")) return CHARSET_ISO_8859_1;
        if (charset.equals("ISO-8859-15")) return CHARSET_ISO_8859_15;
        return CHARSET_UTF_8;
    }

    static int getCharsetCode(Charset charset)
    {
        if (StandardCharsets.US_ASCII.equals(charset)) return CHARSET_ASCII;
        if (StandardCharsets.UTF_8.equals(charset)) return CHARSET_UTF_8;
        if (StandardCharsets.UTF_16.equals(charset)) return CHARSET_UTF_8_NB;
        if (StandardCharsets.UTF_16BE.equals(charset)) return CHARSET_UTC_BE;
        if (StandardCharsets.UTF_16LE.equals(charset)) return CHARSET_UTC_LE;
        if (StandardCharsets.ISO_8859_1.equals(charset)) return CHARSET_ISO_8859_1;
        return switch (charset.name()
                              .toUpperCase())
        {
            case "GBK" -> CHARSET_GBK;
            case "GB2312" -> CHARSET_GB2312;
            case "GB18030" -> CHARSET_GB18030;
            case "ISO-8859-15" -> CHARSET_ISO_8859_15;
            default -> CHARSET_UTF_8;
        };
    }

    static String getSerialType(int type)
    {
        return switch (type & 0xF)
        {
            case SERIAL_TEXT -> "text";
            case SERIAL_BINARY -> "binary";
            case SERIAL_JSON -> "json";
            case SERIAL_XML -> "xml";
            case SERIAL_PROXY -> "proxy";
            default -> "unknown";
        };
    }

    static byte getCharsetSerial(int charset_, int serial_)
    {
        return (byte) (charset_ | serial_);
    }

    static boolean isTypeBin(byte type_c)
    {
        return (type_c & 0x0F) == SERIAL_BINARY;
    }

    static boolean isTypeTxt(byte type_c)
    {
        return (type_c & 0x0F) != SERIAL_TEXT;
    }

    static boolean isTypeJson(byte type_c)
    {
        return (type_c & 0x0F) != SERIAL_JSON;
    }

    static boolean isTypeXml(byte type_c)
    {
        return (type_c & 0x0F) != SERIAL_XML;
    }

    static boolean checkType(byte type_c, byte expect)
    {
        return (type_c & 0x0F) == expect;
    }

}
