/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.bishop.io.mqtt.control;

import static com.tgx.chess.queen.io.core.inf.IQoS.Level.AT_LEAST_ONCE;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import com.tgx.chess.bishop.io.mqtt.QttCommand;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.inf.IConsistent;

/**
 * @author william.d.zk
 * @date 2019-05-30
 */
public class X118_QttSubscribe
        extends
        QttCommand
        implements
        IConsistent
{

    public final static int COMMAND = 0x118;

    public X118_QttSubscribe()
    {
        super(COMMAND);
        setCtrl(generateCtrl(false, false, AT_LEAST_ONCE, QTT_TYPE.SUBSCRIBE));
    }

    private Map<String,
                Level> mSubscribes;

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_06_META_CREATE;
    }

    @Override
    public int dataLength()
    {
        int length = super.dataLength();
        if (mSubscribes != null) {
            for (Map.Entry<String,
                           Level> entry : mSubscribes.entrySet())
            {
                String topic = entry.getKey();
                //2byte UTF-8 length 1byte Qos-lv
                length += 3 + topic.getBytes(StandardCharsets.UTF_8).length;
            }
        }
        return length;
    }

    public Map<String,
               Level> getSubscribes()
    {
        return mSubscribes;
    }

    public void addSubscribe(String topic, Level level)
    {
        if (mSubscribes == null) {
            mSubscribes = new TreeMap<>();
        }
        mSubscribes.put(topic, level);
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        pos = super.decodec(data, pos);

        while (pos < data.length) {
            int utfSize = IoUtil.readUnsignedShort(data, pos);
            pos += 2;
            String topic = IoUtil.readString(data, pos, utfSize, StandardCharsets.UTF_8);
            pos += utfSize;
            Level level = Level.valueOf(data[pos++]);
            addSubscribe(topic, level);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos = super.encodec(data, pos);
        if (mSubscribes != null) {
            for (Map.Entry<String,
                           Level> entry : mSubscribes.entrySet())
            {
                byte[] topic = entry.getKey()
                                    .getBytes(StandardCharsets.UTF_8);
                Level level = entry.getValue();
                pos += IoUtil.writeShort(topic.length, data, pos);
                pos += IoUtil.write(topic, data, pos);
                pos += IoUtil.writeByte(level.ordinal(), data, pos);
            }
        }
        return pos;
    }

    @Override
    public String toString()
    {
        return String.format("subscribe local-id:%d topics:%s",
                             getLocalId(),
                             mSubscribes != null ? mSubscribes.toString()
                                                 : null);
    }

    @Override
    public long getOrigin()
    {
        return getSession().getIndex();
    }
}
