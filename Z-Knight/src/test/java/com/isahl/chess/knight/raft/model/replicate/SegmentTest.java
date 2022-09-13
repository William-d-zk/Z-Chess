/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.knight.raft.model.replicate;

import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;

class SegmentTest
{
    @Test
    void testCreate() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        Segment segment = new Segment(new File(String.format(Segment.fileNameFormatter(false), 1, 0)), 1, true);
        LogEntry entry = new LogEntry(1,
                                      segment.getEndIndex() + 1,
                                      9981234,
                                      9123,
                                      0x99123,
                                      new byte[]{ 1,
                                                  2,
                                                  3,
                                                  4 });
        segment.add(entry);
        segment.freeze();
    }

    @Test
    void testLoad() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        Segment segment = new Segment(new File(String.format(Segment.fileNameFormatter(false), 1, 0)), 1, true);
    }

    @Test
    void testTruncate() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {

    }

    private List<LogEntry> mockEntryInput()
    {
        long term = 1;
        long index = MIN_START;
        List<LogEntry> logs = new ArrayList<>();
        for(long size = 10 + MIN_START; index <= size; index++) {
            LogEntry entry = new LogEntry(index,
                                          term,
                                          0xC002000000000000L,
                                          0x6079376BC6400L,
                                          QttFactory._Instance.serial(),
                                          new byte[]{ (byte) 0x82,
                                                      0x66,
                                                      (byte) 0xE2,
                                                      0x00,
                                                      0x04,
                                                      0x74,
                                                      0x65,
                                                      0x73,
                                                      0x74,
                                                      0x00 });
        }
        return logs;
    }
}