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
package com.tgx.z.queen.base.util;

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

    static String getCharset(byte data) {
        String charset;
        switch (data & 0xF0) {
            case CHARSET_ASCII:
                charset = "ASCII";
                break;
            case CHARSET_UTF_8:
                charset = "UTF-8";
                break;
            case CHARSET_UTF_8_NB:
                charset = "UTF-16";
                break;
            case CHARSET_UTC_BE:
                charset = "UTF-16BE";
                break;
            case CHARSET_UTC_LE:
                charset = "UTF-16LE";
                break;
            case CHARSET_GBK:
                charset = "GBK";
                break;
            case CHARSET_GB2312:
                charset = "GB2312";
                break;
            case CHARSET_GB18030:
                charset = "GB18030";
                break;
            case CHARSET_ISO_8859_1:
                charset = "ISO-8859-1";
                break;
            default:
                charset = "UTF-8";
                break;
        }
        return charset;
    }

    static int getCharsetCode(String charset) {
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

    static String getSerialType(int type) {
        switch (type) {
            case SERIAL_TEXT:
                return "text";
            case SERIAL_BINARY:
                return "binary";
            case SERIAL_JSON:
                return "json";
            case SERIAL_XML:
                return "xml";
            case SERIAL_PROXY:
                return "proxy";
            default:
                return "unknow";
        }
    }

    static byte getCharsetSerial(int charset_, int serial_) {
        return (byte) (charset_ | serial_);
    }

    static boolean isTypeBin(byte type_c) {
        return (type_c & 0x0F) == SERIAL_BINARY;
    }

    static boolean isTypeTxt(byte type_c) {
        return (type_c & 0x0F) != SERIAL_TEXT;
    }

    static boolean isTypeJson(byte type_c) {
        return (type_c & 0x0F) != SERIAL_JSON;
    }

    static boolean isTypeXml(byte type_c) {
        return (type_c & 0x0F) != SERIAL_XML;
    }

    static boolean checkType(byte type_c, byte expect) {
        return (type_c & 0x0F) == expect;
    }

}
