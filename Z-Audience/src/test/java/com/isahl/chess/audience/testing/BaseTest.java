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

import org.junit.jupiter.api.BeforeEach;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseTest
{

    @BeforeEach
    void setUp()
    {
        beforeEach();
    }

    protected void beforeEach()
    {
    }

    protected <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable executable)
    {
        assertThrows(expectedType, executable, "Expected exception was not thrown");
    }

    protected <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable executable, String message)
    {
        assertThrows(expectedType, executable, () -> new AssertionError(message));
    }

    protected <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable executable, Supplier<String> messageSupplier)
    {
        assertThrows(expectedType, executable, messageSupplier);
    }

    protected void assertNotBlank(String string)
    {
        assertNotNull(string);
        assertFalse(string.isBlank(), "String should not be blank");
    }

    protected void assertNotBlank(String string, String message)
    {
        assertNotNull(string, message);
        assertFalse(string.isBlank(), message);
    }

    protected void assertBlank(String string)
    {
        assertTrue(string == null || string.isBlank(), "String should be blank");
    }

    protected void assertLength(int expected, String string)
    {
        assertEquals(expected, string.length(), "String length mismatch");
    }

    protected void assertArrayNotEmpty(byte[] array)
    {
        assertNotNull(array, "Array should not be null");
        assertTrue(array.length > 0, "Array should not be empty");
    }

    protected void assertArrayEquals(byte[] expected, byte[] actual)
    {
        assertNotNull(expected, "Expected array should not be null");
        assertNotNull(actual, "Actual array should not be null");
        assertArrayEquals(expected, actual, "Arrays are not equal");
    }

    protected void assertHexEquals(String expected, byte[] actual)
    {
        assertNotNull(actual, "Actual bytes should not be null");
        StringBuilder sb = new StringBuilder();
        for(byte b : actual)
        {
            sb.append(String.format("%02x", b));
        }
        assertEquals(expected, sb.toString(), "Hex string mismatch");
    }
}
