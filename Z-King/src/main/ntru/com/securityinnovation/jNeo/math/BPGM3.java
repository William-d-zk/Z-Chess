/*
 * NTRU Cryptography Reference Source Code
 *  Copyright (c) 2009-2013, by Security Innovation, Inc. All rights reserved
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,z
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 *  USA.
 */

package com.securityinnovation.jNeo.math;

import com.securityinnovation.jNeo.inputstream.IGF2;

/**
 * This class implements the BPGM3 algorithm defined in the X9.98 standard,
 * with a few modifications.
 * <p>
 * The main routine of this class requires that the IGF be initialized
 * before entry. This is to allow testing with known inputs.
 * <p>
 * Also this implementation allows the number of "+1" coefficients
 * to be different from the number of "-1" coefficients. This was done
 * to support some experiments with the BPGM3.
 */
public class BPGM3
{
    /**
     * Generate a trinomial of degree N-1 that has <code>numOnes</code>
     * coeffcients set to +1 and <code>numNegOnes</code> coefficients
     * set to -1, and all other coefficients set to 0.
     */
    public static FullPolynomial genTrinomial(int N, int numOnes, int numNegOnes, IGF2 igf) {
        boolean isSet[] = new boolean[N];
        for (int i = 0; i < N; i++)
            isSet[i] = false;

        FullPolynomial p = new FullPolynomial(N);
        int t = 0;
        while (t < numOnes) {
            int i = igf.nextIndex();
            if (isSet[i]) continue;

            p.p[i] = 1;
            isSet[i] = true;
            t++;
        }

        t = 0;
        while (t < numNegOnes) {
            int i = igf.nextIndex();
            if (isSet[i]) {
                continue;
            }
            p.p[i] = -1;
            isSet[i] = true;
            t++;
        }

        return p;
    }
}
