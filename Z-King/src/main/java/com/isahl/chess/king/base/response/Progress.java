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

package com.isahl.chess.king.base.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum Progress
{

    /**
     *
     */
    NA("NA/NA")
    {
        @Override
        public int getCount(String formatted)
        {
            return 0;
        }

        @Override
        public int getSize(String formatted)
        {
            return 0;
        }
    },
    /**
     *
     */
    NORMAL("%d/%d")
    {
        @Override
        public int getCount(String formatted)
        {
            String[] split = formatted.split(getSplit());
            return "NA".equals(split[0]) ? 0: Integer.parseInt(split[0]);
        }

        @Override
        public int getSize(String formatted)
        {
            String[] split = formatted.split(getSplit());
            return "NA".equals(split[1]) ? 0: Integer.parseInt(split[1]);
        }
    };

    private final String _Formatter;

    public abstract int getCount(String formatted);

    public abstract int getSize(String formatted);

    Progress(String str)
    {
        _Formatter = str;
    }

    public String getFormatter()
    {
        return _Formatter;
    }

    public String format(int count, int size)
    {
        return String.format(_Formatter, count, size);
    }

    public String getSplit()
    {
        return "/";
    }
}
