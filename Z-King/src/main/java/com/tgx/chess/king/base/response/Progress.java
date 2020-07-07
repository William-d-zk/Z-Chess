package com.tgx.chess.king.base.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
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
            return "NA".equals(split[0]) ? 0
                                         : Integer.parseInt(split[0]);
        }

        @Override
        public int getSize(String formatted)
        {
            String[] split = formatted.split(getSplit());
            return "NA".equals(split[1]) ? 0
                                         : Integer.parseInt(split[1]);
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
