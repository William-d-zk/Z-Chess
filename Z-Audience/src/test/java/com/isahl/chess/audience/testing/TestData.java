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

package com.isahl.chess.audience.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

public class TestData
{

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NUMERIC = "0123456789";
    private static final Random RANDOM = new Random();

    private TestData()
    {
    }

    public static String randomString(int length)
    {
        return randomString(length, ALPHANUMERIC);
    }

    public static String randomString(int length, String chars)
    {
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++)
        {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String randomNumeric(int length)
    {
        return randomString(length, NUMERIC);
    }

    public static String randomUuid()
    {
        return UUID.randomUUID().toString();
    }

    public static byte[] randomBytes(int length)
    {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static int randomInt(int bound)
    {
        return RANDOM.nextInt(bound);
    }

    public static int randomInt(int min, int max)
    {
        return min + RANDOM.nextInt(max - min);
    }

    public static long randomLong()
    {
        return RANDOM.nextLong();
    }

    public static boolean randomBoolean()
    {
        return RANDOM.nextBoolean();
    }

    public static <T> T randomElement(List<T> list)
    {
        if(list == null || list.isEmpty())
        {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static <T> List<T> randomElements(List<T> list, int count)
    {
        if(list == null || list.isEmpty())
        {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        if(count > list.size())
        {
            count = list.size();
        }
        List<T> result = new ArrayList<>(count);
        List<T> copy = new ArrayList<>(list);
        for(int i = 0; i < count; i++)
        {
            result.add(copy.remove(RANDOM.nextInt(copy.size())));
        }
        return result;
    }

    public static String randomEmail()
    {
        return randomString(8) + "@example.com";
    }

    public static String randomUrl()
    {
        return "https://" + randomString(10) + ".example.com/" + randomString(5);
    }

    public static <T> T[] array(T... elements)
    {
        return elements;
    }

    public static <T> List<T> list(T... elements)
    {
        List<T> result = new ArrayList<>();
        for(T element : elements)
        {
            result.add(element);
        }
        return result;
    }

    public static class Builder<T>
    {

        private final Supplier<T> factory;
        private final List<Consumer<T>> modifiers = new ArrayList<>();

        public Builder(Supplier<T> factory)
        {
            this.factory = factory;
        }

        public Builder<T> with(Consumer<T> modifier)
        {
            modifiers.add(modifier);
            return this;
        }

        public T build()
        {
            T instance = factory.get();
            for(Consumer<T> modifier : modifiers)
            {
                modifier.accept(instance);
            }
            return instance;
        }

        public List<T> build(int count)
        {
            List<T> result = new ArrayList<>();
            for(int i = 0; i < count; i++)
            {
                result.add(build());
            }
            return result;
        }
    }

    @FunctionalInterface
    public interface Consumer<T>
    {
        void accept(T t);
    }

    public static <T> Builder<T> builder(Supplier<T> factory)
    {
        return new Builder<>(factory);
    }
}
