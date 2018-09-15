/*
 * NTRU Cryptography Reference Source Code
 *  Copyright (c) 2009-2013, by Security Innovation, Inc. All rights reserved
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 *  USA.
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
    public ParamSetNotSupportedException(byte oid[]) {
        super("Ntru key parameter set (" + oidToString(oid) + ") is not supported");
    }

    /**
     * Constructs a new exception with the supplied OID's name as
     * the detail message.
     */
    public ParamSetNotSupportedException(OID oid) {
        super("Ntru key parameter set (" + oid + ") is not supported");
    }

    /**
     * Create a string containing the OID as "w.x.y.z".
     */
    private static String oidToString(byte oid[]) {
        String s = "";
        if (oid.length > 0) s += oid[0];
        for (int i = 1; i < oid.length; i++)
            s += "." + (0xff & oid[i]);
        return s;
    }
}
