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

import com.isahl.chess.king.base.log.Logger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import javax.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author William.d.zk
 */
public class CryptoUtil
{
    private static final Logger _Logger = Logger.getLogger(CryptoUtil.class.getSimpleName());

    private final static String        _CHARS         = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private final static char          _PAD           = '=';
    private final        MessageDigest _MD5           = create("MD5");
    private final        MessageDigest _SHA_1         = create("SHA-1");
    private final        MessageDigest _SHA_256       = create("SHA-256");
    private final        Random        _Random        = new Random(((long) Math.E) ^ Instant.now()
                                                                                            .toEpochMilli());
    private final        byte[]        _PasswordChars = "qwertyuiopasdfghjklzxcvbnmQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890,-=+_!~`%&*#@;|/".getBytes(
            StandardCharsets.US_ASCII);
    private final        byte[]        _PasswordWords = "qwertyuiopasdfghjklzxcvbnmQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890".getBytes(StandardCharsets.UTF_8);

    /**
     * xor ^ [0-127]
     *
     * @param src
     * @param xor   key
     * @param xor_s key_1
     * @param xor_e key_2
     */
    public static byte xorArrays(byte[] src, byte xor, byte xor_s, byte xor_e)
    {
        if(src == null || src.length == 0) {return xor;}
        if ((xor_s & 0xFF) != 0xFF || (xor_e & 0xFF) != 0xFF) {
            int length = src.length;
            for (int i = 0; i < length; i++) {
                IoUtil.writeByte((src[i] & 0xFF) ^ xor, src, i);
                xor = (byte) (xor < xor_e ? xor + 1 : xor_s);
            }
        }
        return xor;
    }

    public static int adler32(byte[] buf, int off, int len)
    {
        int s1 = 1 & 0xFFFF;
        int s2 = (1 >> 16) & 0xFFFF;
        len += off;
        for(int j = off; j < len; j++) {
            s1 += (buf[j] & 0xFF);
            s2 += s1;
        }
        s1 = s1 % 0xFFF1;
        s2 = s2 % 0xFFF1;
        return (s2 << 16) & 0xFFFF0000 | s1 & 0xFFFF;
    }

    /**
     * 需要注意，返回值是在int32 空间内，即unsigned short
     *
     * @param buf input buffer
     * @param off offset
     * @param len data length
     * @return crc16
     */
    public static int crc16(byte[] buf, int off, int len)
    {
        int crc = 0xFFFF;
        while(len-- != 0) {
            crc ^= buf[off++] & 0xFF;
            for(int i = 0; i < 8; i++) {
                if((crc & 1) > 0) {
                    crc >>>= 1;
                    crc ^= 0xA001;
                }
                else {crc >>>= 1;}
            }
        }
        return crc;
    }

    public static int crc16_modbus(byte[] buf, int off, int len)
    {
        return IoUtil.swapLhb(crc16(buf, off, len));
    }

    public static int crc32(byte[] buf, int off, int len)
    {
        int crc = 0xFFFFFFFF;
        while(len-- != 0) {
            crc ^= buf[off++] & 0xFF;
            for(int i = 0; i < 8; i++) {
                if((crc & 1) > 0) {
                    crc >>>= 1;
                    crc ^= 0xEDB88320;
                }
                else {crc >>>= 1;}
            }
        }
        return ~crc;
    }

    public static long crc64(byte[] buf, int off, int len)
    {
        long crc = 0xFFFFFFFFFFFFFFFFL;
        while(len-- != 0) {
            crc ^= buf[off++] & 0xFF;
            for(int i = 0; i < 8; i++) {
                if((crc & 1) == 1) {
                    crc >>>= 1;
                    crc ^= 0x95AC9329AC4BC9B5L;
                }
                else {crc >>>= 1;}
            }
        }
        return ~crc;
    }

    public static boolean xorSign(byte[] src, byte sign)
    {
        Objects.requireNonNull(src);
        if(src.length == 0) {return true;}
        byte xor = src[0];
        for(int i = 1; i < src.length; i++) {
            xor ^= src[i];
        }
        return xor == sign;
    }

    public static byte xor(byte[] src)
    {
        byte xor = 0;
        for(byte b : src) {
            xor ^= b;
        }
        return xor;
    }

    public static byte[] base64Decoder(char[] src, int start) throws IOException
    {
        if(src == null || src.length == 0) {return null;}
        char[] four = new char[4];
        int i, l, aux;
        char c;
        boolean padded;
        ByteArrayOutputStream dst = new ByteArrayOutputStream(src.length >> 1);
        while(start < src.length) {
            i = 0;
            do {
                if(start >= src.length) {
                    if(i > 0) {throw new IOException("bad BASE 64 In->");}
                    else {return dst.toByteArray();}
                }
                c = src[start++];
                if(_CHARS.indexOf(c) != -1 || c == _PAD) {four[i++] = c;}
                else if(c != '\r' && c != '\n') {throw new IOException("bad BASE 64 In->");}
            }
            while(i < 4);
            padded = false;
            for(i = 0; i < 4; i++) {
                if(four[i] != _PAD && padded) {throw new IOException("bad BASE 64 In->");}
                else if(!padded && four[i] == _PAD) {padded = true;}
            }
            if(four[3] == _PAD) {
                if(start < src.length) {throw new IOException("bad BASE 64 In->");}
                l = four[2] == _PAD ? 1 : 2;
            }
            else {l = 3;}
            for(i = 0, aux = 0; i < 4; i++) {if(four[i] != _PAD) {aux |= _CHARS.indexOf(four[i]) << (6 * (3 - i));}}

            for(i = 0; i < l; i++) {dst.write((aux >>> (8 * (2 - i))) & 0xFF);}
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
        if(src == null || src.length == 0) {return null;}
        StringBuilder encodeDst = new StringBuilder();
        int lineCounter = 0;
        length = Math.min(start + length, src.length);
        while(start < length) {
            int buffer = 0, byteCounter;
            for(byteCounter = 0; byteCounter < 3 && start < length; byteCounter++, start++) {buffer |= (src[start] & 0xFF) << (16 - (byteCounter << 3));}
            if(wrapAt > 0 && lineCounter == wrapAt) {
                encodeDst.append("\r\n");
                lineCounter = 0;
            }
            char b1 = _CHARS.charAt((buffer << 8) >>> 26);
            char b2 = _CHARS.charAt((buffer << 14) >>> 26);
            char b3 = (byteCounter < 2) ? _PAD : _CHARS.charAt((buffer << 20) >>> 26);
            char b4 = (byteCounter < 3) ? _PAD : _CHARS.charAt((buffer << 26) >>> 26);
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
        if(src == null || src.equals("")) {return null;}
        int maxLine = 76;
        try {
            byte[] encodeData = src.getBytes(charSet);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            char[] charArry;
            for(int i = 0, l = 0; i < encodeData.length; i++) {

                if(encodeData[i] >= '!' && encodeData[i] <= '~' && encodeData[i] != '=') {
                    if(l == maxLine) {
                        buffer.write("=\r\n".getBytes());
                        l = 0;
                    }
                    buffer.write(encodeData[i]);
                    l++;
                }
                else {
                    if(l > maxLine - 3) {
                        buffer.write("=\r\n".getBytes());
                        l = 0;
                    }
                    buffer.write('=');
                    charArry = Integer.toHexString(encodeData[i] & 0xFF)
                                      .toUpperCase()
                                      .toCharArray();
                    if(charArry.length < 2) {buffer.write('0');}
                    for(char c : charArry) {buffer.write(c);}
                    l += 3;
                }

            }
            buffer.flush();
            String result = new String(buffer.toByteArray(), charSet);
            buffer.close();
            return result;
        }
        catch(IOException e) {
            // #debug error
            e.printStackTrace();
        }
        return src;
    }

    public static String quoted_print_Decoding(String src, String charSet)
    {
        if(src == null || src.equals("")) {return null;}
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int length = src.length();
        try {
            boolean canIntParse;
            String encode;
            int wr;
            for(int i = 0, k; i < length; ) {
                k = i + 1;
                canIntParse = src.charAt(i) == '=';
                if(canIntParse) {
                    encode = src.substring(k, i += 3);
                    if(encode.equals("\r\n") || encode.equals("\n")) {continue;}
                    wr = Integer.parseInt(encode, 16);
                }
                else {
                    wr = src.charAt(i++);
                    if(wr < '!' || wr > '~') {continue;}
                }
                baos.write(wr);
            }
            baos.flush();
            return baos.toString(charSet);
        }
        catch(Exception e) {
            // #debug error
            e.printStackTrace();
        }
        finally {
            try {
                baos.close();
            }
            catch(IOException e) {
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
        if(input == null || digestName == null) {throw new NullPointerException();}
        if(input.length < len || offset < 0 || offset >= len) {throw new ArrayIndexOutOfBoundsException();}
        MessageDigest md = switch(digestName.toUpperCase()) {
            case "MD5" -> _MD5;
            case "SHA-1" -> _SHA_1;
            case "SHA-256" -> _SHA_256;
            default -> throw new IllegalArgumentException();
        };
        Objects.requireNonNull(md)
               .reset();
        md.update(input, offset, len);
        return md.digest();
    }

    private MessageDigest create(String name)
    {
        try {
            return MessageDigest.getInstance(name);
        }
        catch(NoSuchAlgorithmException ne) {
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

    public final String md5(String input)
    {
        return IoUtil.bin2Hex(md5(input.getBytes(StandardCharsets.UTF_8)));
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

    public final String sha256(String input)
    {
        return IoUtil.bin2Hex(sha256(input.getBytes(StandardCharsets.UTF_8)));
    }

    public final String randomPassword(int min, int max, boolean onlyWords)
    {
        int passwordLength = _Random.nextInt(max - min) + min;
        byte[] seeds = onlyWords ? _PasswordWords : _PasswordChars;
        byte[] pwdBytes = new byte[passwordLength];
        for(int i = 0; i < passwordLength; i++) {
            pwdBytes[i] = seeds[_Random.nextInt(seeds.length)];
        }
        return new String(pwdBytes, StandardCharsets.UTF_8);
    }

    private final static CryptoUtil _Instance = new CryptoUtil();

    static{
        try{
            Security.addProvider(new BouncyCastleProvider());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static String SHA256(String input)
    {
        return _Instance.sha256(input);
    }

    public static String MD5(String input)
    {
        return _Instance.md5(input);
    }

    public static String SHA1(String input)
    {
        return _Instance.md5(input);
    }

    public static byte[] SHA1(byte[] input)
    {
        return _Instance.sha1(input);
    }

    public static String Password(int min, int max)
    {
        return _Instance.randomPassword(min, max, false);
    }

    public static final String EC_ALGORITHM = "EC";

    public static final String ECIES_ALGORITHM = "ECIES";

    public static final String EC_PROVIDER = "BC";

    public static final String SIGNATURE ="SHA3-256withECDSA";

    /**
     * KeyPairGeneratorSpi，从中可以看出支持的 keySize 就这么几个：192、239、256、224、384、521。
     *
     * @param keySize
     * @return
     */
    public static ASymmetricKeyPair generateEccKeyPair(int keySize) {
        try {
            // 获取指定算法的密钥对生成器
            KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_ALGORITHM, EC_PROVIDER);
            // 初始化密钥对生成器（指定密钥长度, 使用默认的安全随机数源）
            generator.initialize(keySize);
            // 随机生成一对密钥（包含公钥和私钥）
            KeyPair keyPair = generator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            String publicKeyString = Base64.encodeBase64String(publicKey.getEncoded());
            String privateKeyString = Base64.encodeBase64String(privateKey.getEncoded());
            return new ASymmetricKeyPair("EC",publicKeyString, privateKeyString);
        } catch (Throwable t) {
            _Logger.fetal("生成ecc密钥对出现异常, keySize = "+keySize,t);
        }
        return null;
    }

    /**
     * ECC 加密
     *
     * @param publicKeyText 公钥
     * @param data      原文
     * @return　密文
     */
    public static String eccEncrypt(String publicKeyText, String data) {
        try {
            X509EncodedKeySpec x509EncodedKeySpec2 = new X509EncodedKeySpec(Base64.decodeBase64(publicKeyText));
            KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGORITHM,EC_PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec2);
            Cipher cipher = Cipher.getInstance(ECIES_ALGORITHM, EC_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] result = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(result);
        } catch (Throwable t) {
            _Logger.fetal("ecc encryption encounter exception.",t);
        }
        return null;
    }

    /**
     * ECC 解密
     *
     * @param privateKeyText 私钥
     * @param data  　       密文
     * @return　原文
     */
    public static String eccDecrypt(String privateKeyText, String data) {
        try {
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec5 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyText));
            KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGORITHM,EC_PROVIDER);
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec5);
            Cipher cipher = Cipher.getInstance(ECIES_ALGORITHM, EC_PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] result = cipher.doFinal(Base64.decodeBase64(data));
            return new String(result);
        } catch (Throwable t) {
            _Logger.fetal("ecc decryption encounter exception.",t);
        }
        return null;
    }

    /**
     * ecc私钥签名
     *
     * @param privateKey 私钥
     * @param data       原文
     * @return 签名
     */
    public static String eccSign(String privateKey, String data) {
        try {
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec5 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGORITHM);
            PrivateKey key = keyFactory.generatePrivate(pkcs8EncodedKeySpec5);
            Signature signature = Signature.getInstance(SIGNATURE);
            signature.initSign(key);
            signature.update(data.getBytes());
            return new String(Base64.encodeBase64(signature.sign()));
        } catch (Throwable t) {
            _Logger.fetal("ecc sign encounter exception.",t);
        }
        return null;
    }

    /**
     * ecc公钥验签
     *
     * @param publicKey 公钥
     * @param srcData   原文
     * @param sign      签名
     * @return
     */
    public static boolean eccVerify(String publicKey, String srcData, String sign) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGORITHM);
            PublicKey key = keyFactory.generatePublic(keySpec);
            Signature signature = Signature.getInstance(SIGNATURE);
            signature.initVerify(key);
            signature.update(srcData.getBytes());
            return signature.verify(Base64.decodeBase64(sign.getBytes()));
        } catch (Throwable t) {
            _Logger.fetal("ecc signature verify encounter exception.",t);
        }
        return false;
    }


    public static class ASymmetricKeyPair {
        private String algorithm;
        private String publicKey;
        private String privateKey;

        public ASymmetricKeyPair() {
        }

        public ASymmetricKeyPair(String algorithm,String publicKey, String privateKey) {
            this.algorithm = algorithm;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }
}
