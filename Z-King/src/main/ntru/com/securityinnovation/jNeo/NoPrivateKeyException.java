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
 * This exception indicates that a private key operation (decrypt)
 * was attempted on a key that only contains public key material.
 */
public class NoPrivateKeyException
        extends
        NtruException
{
    /**
     * 
     */
    private static final long serialVersionUID = -6635387255322595589L;

    /**
     * Constructs a new exception a default message.
     */
    public NoPrivateKeyException()
    {
        super("The key can only be used for public key operations");
    }
}
