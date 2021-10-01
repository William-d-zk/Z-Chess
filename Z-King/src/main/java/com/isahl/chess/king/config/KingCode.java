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

package com.isahl.chess.king.config;

/**
 * @author William.d.zk
 */
public interface KingCode
{
    int SUCCESS                        = 0x00000;
    int ERROR                          = 0xF0000;
    int SYMMETRIC_KEY_OK               = 0x00100;
    int PLAIN_UNSUPPORTED              = 0x001F0;
    int PLAIN_VERSION_LOWER            = 0x001F1;
    int SYMMETRIC_KEY_REROLL           = 0x001F2;
    int MISS                           = 0xF0001;
    int UNKNOWN                        = 0xFFFFF;
    int ILLEGAL_PARAM                  = 0xF0002;
    int FORBIDDEN                      = 0xF0003;
    int UNAUTHORIZED                   = 0xF0004;
    int LOCKED                         = 0xF000A;
    int LOCAL_FILE_SYSTEM_WRITE_FAILED = 0xF1000;

    static String codeOf(int code)
    {
        return switch(code) {
            case SUCCESS -> "SUCCESS";
            case ERROR -> "ERROR";
            case SYMMETRIC_KEY_OK -> "SYMMETRIC_KEY_OK";
            case PLAIN_UNSUPPORTED -> "PLAIN_UNSUPPORTED";
            case PLAIN_VERSION_LOWER -> "PLAIN_VERSION_LOWER";
            case SYMMETRIC_KEY_REROLL -> "SYMMETRIC_KEY_REROLL";
            case MISS -> "MISS";
            case ILLEGAL_PARAM -> "ILLEGAL_PARAM";
            case FORBIDDEN -> "FORBIDDEN";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case LOCAL_FILE_SYSTEM_WRITE_FAILED -> "LOCAL_FILE_SYSTEM_WRITE_FAILED";
            default -> "UNKNOWN";
        };
    }
}
