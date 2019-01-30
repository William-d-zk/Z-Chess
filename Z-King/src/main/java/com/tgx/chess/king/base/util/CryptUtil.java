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
package com.tgx.chess.king.base.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author William.d.zk
 */
public class CryptUtil
{

    final static String   chars   = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    final static char     pad     = '=';
    private MessageDigest MD5     = create("MD5");
    private MessageDigest SHA_1   = create("SHA-1");
    private MessageDigest SHA_256 = create("SHA-256");

    /**
     * xor ^ <0-127>
     *
     * @param src
     * @param xor
     *            key
     * @param xor_s
     *            key_1
     * @param xor_e
     *            key_2
     */
    public static byte xorArrays(byte[] src, byte xor, byte xor_s, byte xor_e)
    {
        if (src == null || src.length == 0) return xor;
        if ((xor_s & 0xFF) == 0xFF && (xor_e & 0xFF) == 0xFF) return xor;
        else {
            int length = src.length;
            for (int i = 0; i < length; i++) {
                IoUtil.writeByte((src[i] & 0xFF) ^ xor, src, i);
                xor = (byte) (xor < xor_e ? xor + 1
                                          : xor_s);
            }
            return xor;
        }
    }

    public static int adler32(byte[] buf, int off, int len)
    {
        int s1 = 1 & 0xFFFF;
        int s2 = (1 >> 16) & 0xFFFF;
        len += off;
        for (int j = off; j < len; j++) {
            s1 += (buf[j] & 0xFF);
            s2 += s1;
        }
        s1 = s1 % 0xFFF1;
        s2 = s2 % 0xFFF1;
        return (s2 << 16) & 0xFFFF0000 | s1 & 0xFFFF;
    }

    public static int crc32(byte[] buf, int off, int len)
    {
        int crc = 0xFFFFFFFF;
        while (len-- != 0) {
            crc ^= buf[off++] & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) > 0) {
                    crc >>>= 1;
                    crc ^= 0xEDB88320;
                }
                else crc >>>= 1;
            }
        }
        return crc ^ 0xFFFFFFFF;
    }

    public static long crc64(byte[] buf, int off, int len)
    {
        long crc = 0xFFFFFFFFFFFFFFFFL;
        while (len-- != 0) {
            crc ^= buf[off++] & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) == 1) {
                    crc >>>= 1;
                    crc ^= 0x95AC9329AC4BC9B5L;
                }
                else crc >>>= 1;
            }
        }
        return crc ^ 0xFFFFFFFFFFFFFFFFL;
    }

    public static byte[] base64Decoder(char[] src, int start) throws IOException
    {
        if (src == null || src.length == 0) return null;
        char[] four = new char[4];
        int i, l, aux;
        char c;
        boolean padded;
        ByteArrayOutputStream dst = new ByteArrayOutputStream(src.length >> 1);
        while (start < src.length) {
            i = 0;
            do {
                if (start >= src.length) {
                    if (i > 0) throw new IOException("bad BASE 64 In->");
                    else return dst.toByteArray();
                }
                c = src[start++];
                if (chars.indexOf(c) != -1 || c == pad) four[i++] = c;
                else if (c != '\r' && c != '\n') throw new IOException("bad BASE 64 In->");
            }
            while (i < 4);
            padded = false;
            for (i = 0; i < 4; i++) {
                if (four[i] != pad && padded) throw new IOException("bad BASE 64 In->");
                else if (!padded && four[i] == pad) padded = true;
            }
            if (four[3] == pad) {
                if (start < src.length) throw new IOException("bad BASE 64 In->");
                l = four[2] == pad ? 1
                                   : 2;
            }
            else l = 3;
            for (i = 0, aux = 0; i < 4; i++)
                if (four[i] != pad) aux |= chars.indexOf(four[i]) << (6 * (3 - i));

            for (i = 0; i < l; i++)
                dst.write((aux >>> (8 * (2 - i))) & 0xFF);
        }
        dst.flush();
        byte[] result = dst.toByteArray();
        dst.close();
        return result;
    }

    public static String base64Encoder(byte[] src, int start, int wrapAt)
    {
        return base64Encoder(src, start, src.length, wrapAt);
    }

    private static String base64Encoder(byte[] src, int start, int length, int wrapAt)
    {
        if (src == null || src.length == 0) return null;
        StringBuffer encodeDst = new StringBuffer();
        int lineCounter = 0;
        length = start + length > src.length ? src.length
                                             : start + length;
        while (start < length) {
            int buffer = 0, byteCounter;
            for (byteCounter = 0; byteCounter < 3 && start < length; byteCounter++, start++)
                buffer |= (src[start] & 0xFF) << (16 - (byteCounter << 3));
            if (wrapAt > 0 && lineCounter == wrapAt) {
                encodeDst.append("\r\n");
                lineCounter = 0;
            }
            char b1 = chars.charAt((buffer << 8) >>> 26);
            char b2 = chars.charAt((buffer << 14) >>> 26);
            char b3 = (byteCounter < 2) ? pad
                                        : chars.charAt((buffer << 20) >>> 26);
            char b4 = (byteCounter < 3) ? pad
                                        : chars.charAt((buffer << 26) >>> 26);
            encodeDst.append(b1)
                     .append(b2)
                     .append(b3)
                     .append(b4);
            lineCounter += 4;
        }
        return encodeDst.toString();
    }

    public static String quoted_print_Encoding(String src, String charSet)
    {
        if (src == null || src.equals("")) return null;
        int maxLine = 76;
        try {
            byte[] encodeData = src.getBytes(charSet);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            char[] charArry;
            for (int i = 0, l = 0; i < encodeData.length; i++) {

                if (encodeData[i] >= '!' && encodeData[i] <= '~' && encodeData[i] != '=') {
                    if (l == maxLine) {
                        buffer.write("=\r\n".getBytes());
                        l = 0;
                    }
                    buffer.write(encodeData[i]);
                    l++;
                }
                else {
                    if (l > maxLine - 3) {
                        buffer.write("=\r\n".getBytes());
                        l = 0;
                    }
                    buffer.write('=');
                    charArry = Integer.toHexString(encodeData[i] & 0xFF)
                                      .toUpperCase()
                                      .toCharArray();
                    if (charArry.length < 2) buffer.write('0');
                    for (char c : charArry)
                        buffer.write(c);
                    l += 3;
                }

            }
            buffer.flush();
            String result = new String(buffer.toByteArray(), charSet);
            buffer.close();
            return result;
        }
        catch (IOException e) {
            // #debug error
            e.printStackTrace();
        }
        return src;
    }

    public static String quoted_print_Decoding(String src, String charSet)
    {
        if (src == null || src.equals("")) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int length = src.length();
        try {
            boolean canIntParse;
            String encode;
            int wr;
            for (int i = 0, k; i < length;) {
                k = i + 1;
                canIntParse = src.charAt(i) == '=';
                if (canIntParse) {
                    encode = src.substring(k, i += 3);
                    if (encode.equals("\r\n") || encode.equals("\n")) continue;
                    wr = Integer.parseInt(encode, 16);
                }
                else {
                    wr = src.charAt(i++);
                    if (wr < '!' || wr > '~') continue;
                }
                baos.write(wr);
            }
            baos.flush();
            return new String(baos.toByteArray(), charSet);
        }
        catch (Exception e) {
            // #debug error
            e.printStackTrace();
        }
        finally {
            try {
                baos.close();
            }
            catch (IOException e) {
                // #debug error
                e.printStackTrace();
            }
        }
        return null;
    }

    private byte[] digest(String digestName, byte[] input)
    {
        return digest(digestName, input, 0, input.length);
    }

    private byte[] digest(String digestName, byte[] input, int offset, int len)
    {
        if (input == null || digestName == null) throw new NullPointerException();
        if (input.length < len || len <= 0 || offset < 0 || offset >= len) throw new ArrayIndexOutOfBoundsException();
        MessageDigest md;
        switch (digestName.toUpperCase())
        {
            case "MD5":
                md = MD5;
                break;
            case "SHA-1":
                md = SHA_1;
                break;
            case "SHA-256":
                md = SHA_256;
                break;
            default:
                throw new IllegalArgumentException();
        }
        md.reset();
        md.update(input, offset, len);
        return md.digest();
    }

    private MessageDigest create(String name)
    {
        try {
            return MessageDigest.getInstance(name);
        }
        catch (NoSuchAlgorithmException ne) {
            return null;
        }
    }

    public final byte[] md5(byte[] input, int offset, int len)
    {
        return digest("MD5", input, offset, len);
    }

    public final byte[] md5(byte[] input)
    {
        return digest("MD5", input);
    }

    public final byte[] sha1(byte[] input, int offset, int len)
    {
        return digest("SHA-1", input, offset, len);
    }

    public final byte[] sha1(byte[] input)
    {
        return digest("SHA-1", input);
    }

    public final byte[] sha256(byte[] input, int offset, int len)
    {
        return digest("SHA-256", input, offset, len);
    }

    public final byte[] sha256(byte[] input)
    {
        return digest("SHA-256", input);
    }

}
