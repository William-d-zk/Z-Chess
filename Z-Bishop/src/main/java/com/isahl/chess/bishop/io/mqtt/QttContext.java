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

package com.isahl.chess.bishop.io.mqtt;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 */
public class QttContext
        extends
        ZContext<QttContext>
{
    private final static IPair SUPPORT_VERSION = new Pair<>(new String[] { "5.0.0",
                                                                           "3.1.1"
    },
                                                            new int[] { 5,
                                                                        4
                                                            });

    public QttContext(ISessionOption option,
                      ISort<QttContext> sort)
    {
        super(option, sort);
    }

    public static IPair getSupportVersion()
    {
        return SUPPORT_VERSION;
    }

    public static int getLastVersion()
    {
        int[] versions = SUPPORT_VERSION.getSecond();
        return versions[0];
    }

    public static int getCurrentVersion()
    {
        int[] versions = SUPPORT_VERSION.getSecond();
        return versions[1];
    }

    private int mVersion = getCurrentVersion();

    public void setVersion(int version)
    {
        mVersion = version;
    }

    public int getVersion()
    {
        return mVersion;
    }

}
