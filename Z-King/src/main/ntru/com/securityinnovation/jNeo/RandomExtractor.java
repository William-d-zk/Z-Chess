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

import com.securityinnovation.jNeo.inputstream.X982Drbg;

/**
 * This class provides hooks to extract the X982 DRBG from a
 * com.securityinnovation.jNeo.Random object.
 */
public class RandomExtractor
{
    public static X982Drbg extractRNG(Random r) {
        return r.rng;
    }
}
