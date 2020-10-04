/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
    public static FullPolynomial genTrinomial(int N, int numOnes, int numNegOnes, IGF2 igf)
    {
        boolean[] isSet = new boolean[N];
        for (int i = 0; i < N; i++)
            isSet[i] = false;

        FullPolynomial p = new FullPolynomial(N);
        int            t = 0;
        while (t < numOnes)
        {
            int i = igf.nextIndex();
            if (isSet[i]) continue;

            p.p[i] = 1;
            isSet[i] = true;
            t++;
        }

        t = 0;
        while (t < numNegOnes)
        {
            int i = igf.nextIndex();
            if (isSet[i])
            {
                continue;
            }
            p.p[i] = -1;
            isSet[i] = true;
            t++;
        }

        return p;
    }
}
