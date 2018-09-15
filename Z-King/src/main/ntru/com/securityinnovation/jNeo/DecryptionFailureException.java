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
    public DecryptionFailureException() {
        super("Input ciphretext is not encrypted with this key");
    }
}
