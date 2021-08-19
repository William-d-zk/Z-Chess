/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.queen.io.core.inf;

import static com.isahl.chess.queen.io.core.inf.ISession.COUNT_BITS;

public interface IPContext
        extends
        IContext
{
    int lackLength(int length, int target);

    int position();

    int lack();

    void finish();

    void advanceOutState(int state);

    void advanceInState(int state);

    default boolean isProxy()
    {
        return false;
    }

    default void updateOut()
    {
    }

    default void updateIn()
    {
    }

    int DECODE_NULL    = -2 << COUNT_BITS;
    int DECODE_FRAME   = -1 << COUNT_BITS;
    int DECODE_PAYLOAD = 01 << COUNT_BITS;
    int DECODE_ERROR   = 03 << COUNT_BITS;

    int ENCODE_NULL    = -2 << COUNT_BITS;
    int ENCODE_FRAME   = -1 << COUNT_BITS;
    int ENCODE_PAYLOAD = 01 << COUNT_BITS;
    int ENCODE_ERROR   = 03 << COUNT_BITS;

    boolean isInConvert();

    boolean isOutConvert();

    boolean isInFrame();

    boolean isOutFrame();

    int inState();

    int outState();

    boolean isInErrorState();

    boolean isOutErrorState();
}
