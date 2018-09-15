
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
 * This exception indicates that an input ciphertext was the
 * wrong length based on the key parameter set.
 */
public class CiphertextBadLengthException
        extends
        NtruException
{
    /**
     * 
     */
    private static final long serialVersionUID = -455175436087069366L;

    /**
     * Constructs a new exception a default message.
     *
     * @param ctLen
     *            the length of the input ciphertext.
     * @param reqCtLen
     *            the required length of the input ciphertext.
     */
    public CiphertextBadLengthException(int ctLen, int reqCtLen) {
        super("Input ciphertext is wrong length (is " + ctLen + "bytes, should be " + reqCtLen + " bytes)");
    }
}
