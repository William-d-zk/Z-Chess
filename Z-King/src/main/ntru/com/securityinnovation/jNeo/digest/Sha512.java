/******************************************************************************
 * NTRU Cryptography Reference Source Code
 *
 * Copyright (C) 2009-2016  Security Innovation (SI)
 *
 * SI has dedicated the work to the public domain by waiving all of its rights
 * to the work worldwide under copyright law, including all related and
 * neighboring rights, to the extent allowed by law.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * You can copy, modify, distribute and perform the work, even for commercial
 * purposes, all without asking permission. You should have received a copy of
 * the creative commons license (CC0 1.0 universal) along with this program.
 * See the license file for more information. 
 *
 *
 *********************************************************************************/

package com.securityinnovation.jNeo.digest;

import java.util.Arrays;

class Sha512
        extends Digest
{
    /**
     * Get the length of the hash output, in bytes.
     */
    public int getDigestLen()
    {
        return HASH_LEN;
    }

    /**
     * Get the size of the input block for the core hash algorithm in bytes.
     */
    public int getBlockLen()
    {
        return BLOCK_LEN;
    }

    /**
     * Constructor.
     */
    public Sha512()
    {
        reset();
    }

    /**
     * Reinitialize the digest operation, discarding any internal state.
     */
    public void reset()
    {
        byteCount1 = 0;
        byteCount2 = 0;

        xBufOff = 0;
        java.util.Arrays.fill(xBuf, (byte) 0);

        wOff = 0;
        java.util.Arrays.fill(W, 0);

        /*
         * sha-512 initial hash value
         * The first 64 bits of the fractional parts of the square roots
         * of the first eight prime numbers
         */
        H1 = 0x6a09e667f3bcc908L;
        H2 = 0xbb67ae8584caa73bL;
        H3 = 0x3c6ef372fe94f82bL;
        H4 = 0xa54ff53a5f1d36f1L;
        H5 = 0x510e527fade682d1L;
        H6 = 0x9b05688c2b3e6c1fL;
        H7 = 0x1f83d9abfb41bd6bL;
        H8 = 0x5be0cd19137e2179L;
    }

    /**
     * Updates the message digest with new data.
     *
     * @param data   the data to be added.
     * @param offset the start of the data in the array.
     * @param length the number of bytes of data to add.
     */
    public void update(byte[] data, int offset, int length)
    {
        byteCount1 += length;

        // Process any full blocks we get by combining cached input
        // with the new input
        while(xBufOff + length >= xBuf.length) {
            int todo = xBuf.length - xBufOff;
            System.arraycopy(data, offset, xBuf, xBufOff, todo);
            processWord(xBuf, 0);
            length -= todo;
            offset += todo;
            xBufOff = 0;
        }

        // Copy any extra data into the cached input buffer.
        System.arraycopy(data, offset, xBuf, xBufOff, length);
        xBufOff += length;
    }

    /**
     * Completes the digest calculation and returns the result
     * in the supplied array.
     */
    public void finishDigest(byte[] out, int outOffset)
    {
        finish();
        unpackWord(H1, out, outOffset);
        unpackWord(H2, out, outOffset + 8);
        unpackWord(H3, out, outOffset + 16);
        unpackWord(H4, out, outOffset + 24);
        unpackWord(H5, out, outOffset + 32);
        unpackWord(H6, out, outOffset + 40);
        unpackWord(H7, out, outOffset + 48);
        unpackWord(H8, out, outOffset + 56);

        reset();
    }

    // SHA-512 block transform routines
    //......................................................................

    private static final int HASH_LEN = 64;

    private static final int BLOCK_LEN = 128;

    private final byte[] xBuf    = new byte[8];
    private       int    xBufOff = 0;

    private long byteCount1 = 0;
    private long byteCount2 = 0;

    private long H1, H2, H3, H4, H5, H6, H7, H8;

    private final long[] W = new long[80];
    private       int    wOff;

    private void update(byte in)
    {
        xBuf[xBufOff++] = in;

        if(xBufOff == xBuf.length) {
            processWord(xBuf, 0);
            xBufOff = 0;
        }

        byteCount1++;
    }

    public void finish()
    {
        adjustByteCounts();

        long lowBitLength = byteCount1 << 3;
        long hiBitLength = byteCount2;

        // add the pad bytes.
        update((byte) 128);
        while(xBufOff != 0)
            update((byte) 0);
        processLength(lowBitLength, hiBitLength);
        processBlock();
    }

    protected void processWord(byte[] in, int inOff)
    {
        W[wOff++] = (((long) (in[inOff] & 0xff) << 56) | ((long) (in[inOff + 1] & 0xff) << 48) |
                     ((long) (in[inOff + 2] & 0xff) << 40) | ((long) (in[inOff + 3] & 0xff) << 32) |
                     ((long) (in[inOff + 4] & 0xff) << 24) | ((long) (in[inOff + 5] & 0xff) << 16) |
                     ((long) (in[inOff + 6] & 0xff) << 8) | ((long) (in[inOff + 7] & 0xff)));

        if(wOff == 16) {processBlock();}
    }

    protected void unpackWord(long word, byte[] out, int outOff)
    {
        out[outOff] = (byte) (word >>> 56);
        out[outOff + 1] = (byte) (word >>> 48);
        out[outOff + 2] = (byte) (word >>> 40);
        out[outOff + 3] = (byte) (word >>> 32);
        out[outOff + 4] = (byte) (word >>> 24);
        out[outOff + 5] = (byte) (word >>> 16);
        out[outOff + 6] = (byte) (word >>> 8);
        out[outOff + 7] = (byte) word;
    }

    /**
     * adjust the byte counts so that byteCount2 represents the
     * upper long (less 3 bits) word of the byte count.
     */
    private void adjustByteCounts()
    {
        if(byteCount1 > 0x1fffffffffffffffL) {
            byteCount2 += (byteCount1 >>> 61);
            byteCount1 &= 0x1fffffffffffffffL;
        }
    }

    protected void processLength(long lowW, long hiW)
    {
        if(wOff > 14) {processBlock();}

        W[14] = hiW;
        W[15] = lowW;
    }

    protected void processBlock()
    {
        adjustByteCounts();

        // expand 16 word block into 80 word blocks.
        for(int t = 16; t <= 79; t++) {W[t] = Sigma1(W[t - 2]) + W[t - 7] + Sigma0(W[t - 15]) + W[t - 16];}

        //
        // set up working variables.
        //
        long a = H1;
        long b = H2;
        long c = H3;
        long d = H4;
        long e = H5;
        long f = H6;
        long g = H7;
        long h = H8;

        for(int t = 0; t <= 79; t++) {

            long T1, T2;

            T1 = h + Sum1(e) + Ch(e, f, g) + K[t] + W[t];
            T2 = Sum0(a) + Maj(a, b, c);
            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;
        }

        H1 += a;
        H2 += b;
        H3 += c;
        H4 += d;
        H5 += e;
        H6 += f;
        H7 += g;
        H8 += h;

        // reset the offset and clean out the word buffer.
        wOff = 0;
        Arrays.fill(W, 0);
    }

    private long rotateRight(long x, int n)
    {
        return (x >>> n) | (x << (64 - n));
    }

    /* sha-384 and sha-512 functions (as for sha-256 but for longs) */
    private long Ch(long x, long y, long z)
    {
        return ((x & y) ^ ((~x) & z));
    }

    private long Maj(long x, long y, long z)
    {
        return ((x & y) ^ (x & z) ^ (y & z));
    }

    private long Sum0(long x)
    {
        return rotateRight(x, 28) ^ rotateRight(x, 34) ^ rotateRight(x, 39);
    }

    private long Sum1(long x)
    {
        return rotateRight(x, 14) ^ rotateRight(x, 18) ^ rotateRight(x, 41);
    }

    private long Sigma0(long x)
    {
        return rotateRight(x, 1) ^ rotateRight(x, 8) ^ (x >>> 7);
    }

    private long Sigma1(long x)
    {
        return rotateRight(x, 19) ^ rotateRight(x, 61) ^ (x >>> 6);
    }

    /*
     * sha-384 and sha-512 Constants
     * (represent the first 64 bits of the fractional parts of the
     * cube roots of the first sixty-four prime numbers)
     */
    static final long[] K = {
            0x428a2f98d728ae22L,
            0x7137449123ef65cdL,
            0xb5c0fbcfec4d3b2fL,
            0xe9b5dba58189dbbcL,
            0x3956c25bf348b538L,
            0x59f111f1b605d019L,
            0x923f82a4af194f9bL,
            0xab1c5ed5da6d8118L,
            0xd807aa98a3030242L,
            0x12835b0145706fbeL,
            0x243185be4ee4b28cL,
            0x550c7dc3d5ffb4e2L,
            0x72be5d74f27b896fL,
            0x80deb1fe3b1696b1L,
            0x9bdc06a725c71235L,
            0xc19bf174cf692694L,
            0xe49b69c19ef14ad2L,
            0xefbe4786384f25e3L,
            0x0fc19dc68b8cd5b5L,
            0x240ca1cc77ac9c65L,
            0x2de92c6f592b0275L,
            0x4a7484aa6ea6e483L,
            0x5cb0a9dcbd41fbd4L,
            0x76f988da831153b5L,
            0x983e5152ee66dfabL,
            0xa831c66d2db43210L,
            0xb00327c898fb213fL,
            0xbf597fc7beef0ee4L,
            0xc6e00bf33da88fc2L,
            0xd5a79147930aa725L,
            0x06ca6351e003826fL,
            0x142929670a0e6e70L,
            0x27b70a8546d22ffcL,
            0x2e1b21385c26c926L,
            0x4d2c6dfc5ac42aedL,
            0x53380d139d95b3dfL,
            0x650a73548baf63deL,
            0x766a0abb3c77b2a8L,
            0x81c2c92e47edaee6L,
            0x92722c851482353bL,
            0xa2bfe8a14cf10364L,
            0xa81a664bbc423001L,
            0xc24b8b70d0f89791L,
            0xc76c51a30654be30L,
            0xd192e819d6ef5218L,
            0xd69906245565a910L,
            0xf40e35855771202aL,
            0x106aa07032bbd1b8L,
            0x19a4c116b8d2d0c8L,
            0x1e376c085141ab53L,
            0x2748774cdf8eeb99L,
            0x34b0bcb5e19b48a8L,
            0x391c0cb3c5c95a63L,
            0x4ed8aa4ae3418acbL,
            0x5b9cca4f7763e373L,
            0x682e6ff3d6b2b8a3L,
            0x748f82ee5defb2fcL,
            0x78a5636f43172f60L,
            0x84c87814a1f0ab72L,
            0x8cc702081a6439ecL,
            0x90befffa23631e28L,
            0xa4506cebde82bde9L,
            0xbef9a3f7b2c67915L,
            0xc67178f2e372532bL,
            0xca273eceea26619cL,
            0xd186b8c721c0c207L,
            0xeada7dd6cde0eb1eL,
            0xf57d4f7fee6ed178L,
            0x06f067aa72176fbaL,
            0x0a637dc5a2c898a6L,
            0x113f9804bef90daeL,
            0x1b710b35131c471bL,
            0x28db77f523047d84L,
            0x32caab7b40c72493L,
            0x3c9ebe0a15c9bebcL,
            0x431d67c49c100d4cL,
            0x4cc5d4becb3e42b6L,
            0x597f299cfc657e2aL,
            0x5fcb6fab3ad6faecL,
            0x6c44198c4a475817L
    };
}
