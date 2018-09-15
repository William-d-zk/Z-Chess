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
 * This exception indicates that the header tag of an input key blob
 * is not recognized.
 */
public class FormatNotSupportedException
        extends
        NtruException
{
    /**
     * 
     */
    private static final long serialVersionUID = 8537720740509544817L;

    /**
     * Constructs a new exception with the supplied key blob tag as
     * the detail message.
     */
    public FormatNotSupportedException(byte tag) {
        super("Blob format (" + Integer.toHexString(0xff & tag) + ") is not supported");
    }
}
