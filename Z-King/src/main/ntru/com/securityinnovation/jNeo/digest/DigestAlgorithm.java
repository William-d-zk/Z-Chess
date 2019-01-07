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

package com.securityinnovation.jNeo.digest;

/**
 * This class provides an enumeration of hash algorithms that can
 * be used throughout the code. Each enumeration has a utility
 * function for creating a new instance of a Digest object
 * for that algorithm.
 */
public enum DigestAlgorithm
{
    /**
     * The enum for SHA1.
     */
    sha1(Sha1.class),

    /**
     * The enum for SHA256.
     */
    sha256(Sha256.class);

    /**
     * Constructor.
     */
    DigestAlgorithm(Class<? extends Digest> _clss)
    {
        clss = _clss;
    }

    /**
     * The class used to generate objects
     */
    private Class<? extends Digest> clss;

    /**
     * Return the byte array identifying the OID.
     */
    public Digest newInstance()
    {
        try {
            return clss.newInstance();
        }
        // By construction this shouldn't happen,
        // except perhaps an out-of-memory error.
        catch (Exception e) {
            return null;
        }
    }
}
