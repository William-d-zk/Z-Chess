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
 * This exception indicates that the input key blob contains a key that
 * uses an NtruEncrypt or NtruSign parameter set that is not supported
 * by this implementation.
 */
public class ParamSetNotSupportedException
        extends
        NtruException
{
    /**
     * 
     */
    private static final long serialVersionUID = -781847736774356415L;

    /**
     * Constructs a new exception with the supplied OID as the detail message,
     * formatted as "w.x.y.z".
     */
    public ParamSetNotSupportedException(byte[] oid)
    {
        super("Ntru key parameter set (" + oidToString(oid) + ") is not supported");
    }

    /**
     * Constructs a new exception with the supplied OID's name as
     * the detail message.
     */
    public ParamSetNotSupportedException(OID oid)
    {
        super("Ntru key parameter set (" + oid + ") is not supported");
    }

    /**
     * Create a string containing the OID as "w.x.y.z".
     */
    private static String oidToString(byte[] oid)
    {
        String s = "";
        if (oid.length > 0) s += oid[0];
        for (int i = 1; i < oid.length; i++)
            s += "." + (0xff & oid[i]);
        return s;
    }
}
