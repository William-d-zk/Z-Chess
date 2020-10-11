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

package com.isahl.chess.queen.io.core.manager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.util.Pair;

class MixManagerTest
{

    @Test
    void addPeer()
    {
        List<IPair> list = new ArrayList<>();
        add(0, list, new Pair<>(0, new InetSocketAddress(5678)));
    }

    private void add(int index, List<IPair> pairs, IPair pair)
    {
        Objects.requireNonNull(pairs);
        Objects.requireNonNull(pair);
        if (index >= pairs.size())
        {
            for (int i = pairs.size(); i < index; i++)
            {
                pairs.add(new Pair<>(-1, null));
            }
            pairs.add(pair);
        }
        else
        {
            pairs.set(index, pair);
        }
    }
}