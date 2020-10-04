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

package com.securityinnovation.jNeo;

/**
 * This class provides a list of constant identifiers for the
 * NtruEncrypt parameter sets. These constants are for use during
 * key generation, when the application must specify which parameter
 * set to use to create the new keypair.
 * <p>
 * The parameter sets named here are defined in the ANSI X9.98
 * specification.
 */
public enum OID
{
    /**
     * <p>
     * The parameter set identifier for ees401ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>112 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>60 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>552 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>556 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>637 bytes</td>
     * </tr>
     * </table>
     */
    ees401ep1(0, 2, 4),

    /**
     * <p>
     * The parameter set identifier for ees449ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>128 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>67 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>618 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>622 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>712 bytes</td>
     * </tr>
     * </table>
     */
    ees449ep1(0, 3, 3),

    /**
     * <p>
     * The parameter set identifier for ees677ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>192 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>101 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>931 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>935 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1071 bytes</td>
     * </tr>
     * </table>
     */
    ees677ep1(0, 5, 3),

    /**
     * <p>
     * The parameter set identifier for ees1087ep2.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>256 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>170 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>1495 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>1499 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1717 bytes</td>
     * </tr>
     * </table>
     */
    ees1087ep2(0, 6, 3),

    /**
     * <p>
     * The parameter set identifier for ees541ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>112 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>86 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>744 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>748 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>857 bytes</td>
     * </tr>
     * </table>
     */
    ees541ep1(0, 2, 5),

    /**
     * <p>
     * The parameter set identifier for ees613ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>128 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>97 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>843 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>847 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>970 bytes</td>
     * </tr>
     * </table>
     */
    ees613ep1(0, 3, 4),

    /**
     * <p>
     * The parameter set identifier for ees887ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>192 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>141 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>1220 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>1224 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1402 bytes</td>
     * </tr>
     * </table>
     */
    ees887ep1(0, 5, 4),

    /**
     * <p>
     * The parameter set identifier for ees1171ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>256 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>186 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>1611 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>1615 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1850 bytes</td>
     * </tr>
     * </table>
     */
    ees1171ep1(0, 6, 4),

    /**
     * <p>
     * The parameter set identifier for ees659ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>112 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>108 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>907 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>911 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1006 bytes</td>
     * </tr>
     * </table>
     */
    ees659ep1(0, 2, 6),

    /**
     * <p>
     * The parameter set identifier for ees761ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>128 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>125 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>1047 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>1051 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1156 bytes</td>
     * </tr>
     * </table>
     */
    ees761ep1(0, 3, 5),

    /**
     * <p>
     * The parameter set identifier for ees1087ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>192 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>178 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>1495 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>1499 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>1673 bytes</td>
     * </tr>
     * </table>
     */
    ees1087ep1(0, 5, 5),

    /**
     * <p>
     * The parameter set identifier for ees1499ep1.
     * <p>
     * This parameter set has the following properties:
     * <table border=1>
     * <tr>
     * <td>security level</td>
     * <td>256 bits</td>
     * </tr>
     * <tr>
     * <td>max plaintext length</td>
     * <td>247 bytes</td>
     * </tr>
     * <tr>
     * <td>ciphertext length</td>
     * <td>2062 bytes</td>
     * </tr>
     * <tr>
     * <td>public key blob length</td>
     * <td>2066 bytes</td>
     * </tr>
     * <tr>
     * <td>private key blob length</td>
     * <td>2284 bytes</td>
     * </tr>
     * </table>
     */
    ees1499ep1(0, 6, 5);

    /**
     * Constructor. This constructor assumes there will be 3 bytes
     * in the OID.
     */
    OID(int first, int second, int third)
    {
        oidBytes = new byte[3];
        oidBytes[0] = (byte) first;
        oidBytes[1] = (byte) second;
        oidBytes[2] = (byte) third;
    }

    /**
     * The byte sequence identifying the OID.
     */
    byte[] oidBytes;

    /**
     * Return the byte array identifying the OID.
     */
    public byte[] getOIDBytes()
    {
        return oidBytes;
    }
}
