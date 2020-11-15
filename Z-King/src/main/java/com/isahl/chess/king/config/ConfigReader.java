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

package com.isahl.chess.king.config;

import java.io.Serializable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.isahl.chess.king.base.util.RegExUtil;

/**
 * @author William.d.zk
 */
public class ConfigReader
        implements
        Serializable
{

    /**
     * 读取配置文件信息
     * 
     * @param name
     *            读取节点名
     * @param fileName
     *            文件名
     * 
     * @return 读取的节点值
     */
    public static String readConfigString(String name, String fileName) throws MissingResourceException,
                                                                        ClassCastException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return rb.getString(name);
    }

    public static boolean readConfigBoolean(String name, String fileName) throws MissingResourceException,
                                                                          ClassCastException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Boolean.parseBoolean(rb.getString(name));
    }

    public static int readConfigInteger(String name, String fileName) throws MissingResourceException,
                                                                      ClassCastException,
                                                                      NumberFormatException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Integer.parseInt(rb.getString(name));
    }

    public static int readConfigHexInteger(String name, String fileName) throws MissingResourceException,
                                                                         ClassCastException,
                                                                         NumberFormatException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Integer.parseInt(rb.getString(name), 16);
    }

    public static long readConfigHexLong(String name, String fileName) throws MissingResourceException,
                                                                       ClassCastException,
                                                                       NumberFormatException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Long.parseLong(rb.getString(name), 16);
    }

    public static long readConfigLong(String name, String fileName) throws MissingResourceException,
                                                                    ClassCastException,
                                                                    NumberFormatException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Long.parseLong(rb.getString(name));
    }

    public static double readConfigDouble(String name, String fileName) throws MissingResourceException,
                                                                        ClassCastException,
                                                                        NumberFormatException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Double.parseDouble(rb.getString(name));
    }

    public static float readConfigFloat(String name, String fileName) throws MissingResourceException,
                                                                      ClassCastException,
                                                                      NumberFormatException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        return Float.parseFloat(rb.getString(name));
    }

    public static String[] readConfigStringArray(String name, String fileName) throws MissingResourceException,
                                                                               ClassCastException
    {
        return readConfigArray(name, fileName, RegExUtil.STRING_ARRAY_PATTERN, RegExUtil.STRING_PATTERN);
    }

    public static int[] readConfigIntegerArray(String name, String fileName) throws MissingResourceException,
                                                                             ClassCastException
    {
        String[] result = readConfigArray(name, fileName, RegExUtil.INTEGER_ARRAY_PATTERN, RegExUtil.INTEGER_PATTERN);
        int[] intArray = new int[result.length];
        if (result.length > 0) {
            for (int i = 0, size = result.length; i < size; i++) {
                intArray[i] = Integer.parseInt(result[i]);
            }
        }
        return intArray;
    }

    public static long[] readConfigLongArray(String name, String fileName) throws MissingResourceException,
                                                                           ClassCastException
    {
        String[] result = readConfigArray(name, fileName, RegExUtil.INTEGER_ARRAY_PATTERN, RegExUtil.INTEGER_PATTERN);
        long[] longArray = new long[result.length];
        if (result.length > 0) {
            for (int i = 0, size = result.length; i < size; i++) {
                longArray[i] = Long.parseLong(result[i]);
            }
        }
        return longArray;
    }

    public static double[] readConfigDoubleArray(String name, String fileName) throws MissingResourceException,
                                                                               ClassCastException
    {
        String[] result = readConfigArray(name, fileName, RegExUtil.DOUBLE_ARRAY_PATTERN, RegExUtil.DOUBLE_PATTERN);
        double[] doubleArray = new double[result.length];
        if (result.length > 0) {
            for (int i = 0, size = result.length; i < size; i++) {
                doubleArray[i] = Double.parseDouble(result[i]);
            }
        }
        return doubleArray;
    }

    public static float[] readConfigFloatArray(String name, String fileName) throws MissingResourceException,
                                                                             ClassCastException
    {
        String[] result = readConfigArray(name, fileName, RegExUtil.DOUBLE_ARRAY_PATTERN, RegExUtil.DOUBLE_PATTERN);
        float[] floatArray = new float[result.length];
        if (result.length > 0) {
            for (int i = 0, size = result.length; i < size; i++) {
                floatArray[i] = Float.parseFloat(result[i]);
            }
        }
        return floatArray;
    }

    private static String[] readConfigArray(String name,
                                            String fileName,
                                            Pattern pattern,
                                            Pattern innerPattern) throws MissingResourceException, ClassCastException
    {
        ResourceBundle rb = ResourceBundle.getBundle(fileName);
        String line = rb.getString(name);
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String matched = matcher.group()
                                    .replace("{", "")
                                    .replace("}", "");
            String[] splits = matched.split(",");
            for (String x : splits) {
                if (!innerPattern.matcher(x)
                                 .matches())
                {
                    throw new IllegalArgumentException("Check " + name + " @ " + x);
                }
            }
            return splits;
        }
        throw new IllegalArgumentException("Check " + name);
    }

    public static Object readObject(ResourceBundle resourceBundle, String name)
    {
        String value = resourceBundle.getString(name);
        if (value.matches(RegExUtil.BOOLEAN_PATTERN_STRING)) {
            return value.matches(RegExUtil.BOOLEAN_TRUE_PATTERN_STRING) ? Boolean.TRUE
                                                                        : Boolean.FALSE;
        }
        else if (value.matches(RegExUtil.INTEGER_PATTERN_STRING)) {
            return Integer.parseInt(value);
        }
        else if (value.matches(RegExUtil.DOUBLE_PATTERN_STRING)) { return Double.parseDouble(value); }
        return value;
    }

}
