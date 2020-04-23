/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.securityinnovation.jNeo;

/**
 * This exception indicates that a decryption operation failed. This
 * may be because the ciphertext has been corrupted or because the
 * wrong key was used. This exception is not used if the corrupt
 * ciphertext prevents the decryption calculation from even being
 * performed (for example, if the NtruEncrypt ciphertext is the wrong
 * length). It is used only if the decryption can proceed but fails
 * due to an internal error check, such as a CCM MAC verification
 * failure or an NtruEncrypt decryption candidate having the wrong
 * format.
 */
public class DecryptionFailureException
        extends
        NtruException
{
    /**
     * 
     */
    private static final long serialVersionUID = 5414525923421373848L;

    /**
     * Constructs a new exception a default message.
     */
    public DecryptionFailureException()
    {
        super("Input ciphretext is not encrypted with this key");
    }
}
