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

package com.isahl.chess.king.base.util;

import java.util.regex.Pattern;

/**
 * @author william.d.zk
 * @date 2018/1/3
 */
public class RegExUtil
{
    public static String  BOOLEAN_PATTERN_STRING      =
            "([tT][rR][uU][eE])|" + "([fF][aA][lL][sS][eE])|" + "([yY][eE][sS])|" + "([nN][oO])|" +
            "([eE][nN][aA][bB][lL][eE])|" + "([dD][iI][sS][aA][bB][lL][eE])";
    public static String  BOOLEAN_TRUE_PATTERN_STRING =
            "([tT][rR][uU][eE])|" + "([yY][eE][sS])|" + "([eE][nN][aA][bB][lL][eE])";
    public static String  INTEGER_PATTERN_STRING      = "([-+]?\\d+\\s*,?)";
    public static String  DOUBLE_PATTERN_STRING       = "(([-+]?((\\d+(\\.\\d*)?)|(\\.\\d+)))([eE](([-+]?([012]?\\d{1,2}|30[0-7]))|-3([01]?[4-9]|[012]?[0-3])))?[dD]?\\s*,?)";
    public static String  STRING_PATTERN_STRING       = "(\\w+\\s*,?)";
    public static Pattern BOOLEAN_PATTERN             = Pattern.compile(BOOLEAN_PATTERN_STRING);
    public static Pattern INTEGER_PATTERN             = Pattern.compile(INTEGER_PATTERN_STRING);
    public static Pattern INTEGER_ARRAY_PATTERN       = Pattern.compile("\\{(\\s*" + INTEGER_PATTERN_STRING + "*)}");
    public static Pattern DOUBLE_PATTERN              = Pattern.compile(DOUBLE_PATTERN_STRING);
    public static Pattern DOUBLE_ARRAY_PATTERN        = Pattern.compile("\\{(\\s*" + DOUBLE_PATTERN_STRING + "*)}");
    public static Pattern STRING_PATTERN              = Pattern.compile(STRING_PATTERN_STRING);
    public static Pattern STRING_ARRAY_PATTERN        = Pattern.compile("\\{(\\s*" + STRING_PATTERN_STRING + "*)}");

    public static Class<?> testType(String value)
    {
        if(value.matches(BOOLEAN_PATTERN_STRING)) {
            return Boolean.TYPE;
        }
        else if(value.matches(INTEGER_PATTERN_STRING)) {
            return Integer.TYPE;
        }
        else if(value.matches(DOUBLE_PATTERN_STRING)) {return Double.TYPE;}
        return String.class;
    }

    public static String splitFormatter(String regex, int count)
    {
        StringBuilder sb = new StringBuilder();
        for(int i = 0, size = count - 1; i < size; i++) {
            sb.append("%s")
              .append(regex);
        }
        sb.append("%s");
        return sb.toString();
    }
}
