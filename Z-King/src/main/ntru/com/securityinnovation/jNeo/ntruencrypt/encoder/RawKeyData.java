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

package com.securityinnovation.jNeo.ntruencrypt.encoder;

import com.securityinnovation.jNeo.math.FullPolynomial;
import com.securityinnovation.jNeo.ntruencrypt.KeyParams;

/**
 * This class holds the result of parsing a key blob. The
 * result contains the parameter set, the public key, and the
 * private key (which will be null if the input blob was a public
 * key blob).
 */
public class RawKeyData
{
    public KeyParams      keyParams;
    public FullPolynomial h;
    public FullPolynomial f;

    public RawKeyData(KeyParams _keyParams, FullPolynomial _h)
    {
        keyParams = _keyParams;
        h         = _h;
        f         = null;
    }

    public RawKeyData(KeyParams _keyParams, FullPolynomial _h, FullPolynomial _f)
    {
        keyParams = _keyParams;
        h         = _h;
        f         = _f;
    }
}
