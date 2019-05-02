/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.bishop.io.mqtt.bean;

import com.tgx.chess.queen.io.core.async.AioContext;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

public class QttContext
        extends
        AioContext
{
    public enum HEAD
    {

        CONNECT(1, "C->S", "Client request to connect to Server"),
        CONNACK(2, "S->C", "Connect acknowledgment"),
        PUBLISH(3, "C->S | S->C", "Publish message"),
        PUBACK(4, "C->S | S->C", "Publish acknowledgment"),
        PUBREC(5, "C->S | S->C", "Publish received (assured delivery part 1)"),
        PUBREL(6, "C->S | S->C", "Publish release (assured delivery part 2)"),
        PUBCOMP(7, "C->S | S->C", "Publish complete (assured delivery part 3)"),
        SUBSCRIBE(8, "C->S", "Client subscribe request"),
        SUBACK(9, "S->C", "Subscribe acknowledgment"),
        UNSUBSCRIBE(10, "C->S", "Unsubscribe request"),
        UNSUBACK(11, "S->C", "Unsubscribe acknowledgment"),
        PINGREQ(12, "C->S", "PING request"),
        PINGRESP(13, "S->C", "PING response"),
        DISCONNECT(14, "C->S", "Client is disconnecting");

        final int    _Value;
        final String _Description;
        final String _Flow;

        HEAD(int code,
             String flow,
             String description)
        {
            _Value = code;
            _Flow = flow;
            _Description = description;
        }

        public int getValue()
        {
            return _Value;
        }

        public String getFlow()
        {
            return _Flow;
        }

        public String getDescription()
        {
            return _Description;
        }

    }

    public QttContext(ISessionOption option)
    {
        super(option);
        transfer();
    }

    public String getVersion()
    {
        return "3.1.1";
    }
}
