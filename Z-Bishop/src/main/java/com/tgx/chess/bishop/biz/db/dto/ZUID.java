/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.chess.bishop.biz.db.dto;

import java.time.Instant;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import com.tgx.chess.king.base.log.Logger;

/**
 * 00-0000-000-0000000-0000000000000000000000000000000000000-000000000
 * -2bit-
 * 10 Client manager service
 * 11 Cluster symmetry communication
 * 01 Internal message queue broker
 * 00 Device consumer connection
 * -4bit-
 * Cluster region
 * -3bit-
 * Cluster set identity
 * -7bit-
 * Endpoint identity
 * -38bit-
 * Timestamp gap 2018-06-01 00:00:00.000
 * -10bit-
 * sequence in one millisecond
 */
public class ZUID
{
    private final Logger         _Log               = Logger.getLogger(getClass().getName());
    private static final long    TWEPOCH            = Instant.parse("2018-06-01T00:00:00.00Z")
                                                             .toEpochMilli();
    private static final int     MAX_IDC_ID         = 15;
    private static final int     MAX_CLUSTER_SET_ID = 7;
    private static final int     MAX_NODE_ID        = 127;
    private static final int     SEQUENCE_BITS      = 10;
    private static final long    SEQUENCE_MASK      = ~(-1L << SEQUENCE_BITS);
    private static final int     TIMESTAMP_BITS     = 38;
    private static final int     TIMESTAMP_SHIFT    = SEQUENCE_BITS;
    private static final int     NODE_BITS          = 7;
    private static final int     NODE_SHIFT         = TIMESTAMP_SHIFT + TIMESTAMP_BITS;
    private static final int     CLUSTER_BITS       = 3;
    private static final int     CLUSTER_SHIFT      = NODE_SHIFT + NODE_BITS;
    private static final int     IDC_BITS           = 4;
    private static final int     IDC_SHIFT          = CLUSTER_SHIFT + CLUSTER_BITS;
    private static final int     TYPE_BITS          = 2;
    private static final int     TYPE_SHIFT         = IDC_SHIFT + IDC_BITS;

    private final long           _IdcId;
    private final long           _ClusterId;
    private final long           _NodeId;
    private final long           _Type;
    private final Supplier<Long> _TimestampSupplier;
    private long                 sequence;
    private long                 lastTimestamp;

    public ZUID() {
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("zuid");
            for (String key : bundle.keySet()) {
                _Log.info("zuid %s:%s", key, bundle.getString(key));
            }
        }
        catch (MissingResourceException e) {
            throw new RuntimeException("FETAL ERROR: ZUID properites is missing !");
        }
        _IdcId = Long.parseLong(bundle.getString("idc.id"));
        _ClusterId = Long.parseLong(bundle.getString("cluster.id"));
        _NodeId = Long.parseLong(bundle.getString("node.id"));
        _Type = Long.parseLong(bundle.getString("type"));
        _TimestampSupplier = System::currentTimeMillis;
    }

    public ZUID(long idc_id, long cluster_id, long node_id, long type) {
        if (idc_id > MAX_IDC_ID
            || idc_id < 0) { throw new IllegalArgumentException(String.format("idc region Id can't be greater than %d or less than 0",
                                                                              MAX_IDC_ID)); }
        if (cluster_id > MAX_CLUSTER_SET_ID
            || cluster_id < 0) { throw new IllegalArgumentException(String.format("cluster Id can't be greater than %d or less than 0",
                                                                                  MAX_CLUSTER_SET_ID)); }
        if (node_id > MAX_NODE_ID
            || node_id < 0) { throw new IllegalArgumentException(String.format("node Id can't be greater than %d or less than 0",
                                                                               MAX_NODE_ID)); }
        _IdcId = idc_id;
        _ClusterId = cluster_id;
        _NodeId = node_id;
        _Type = type;
        _TimestampSupplier = System::currentTimeMillis;
    }

    public synchronized long getId() {
        long timestamp = _TimestampSupplier.get();
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                LockSupport.parkUntil(timestamp + 1);
            }
        }
        else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return (_Type << TYPE_SHIFT)
               | (_IdcId << IDC_SHIFT)
               | (_ClusterId << CLUSTER_SHIFT)
               | (_NodeId << NODE_SHIFT)
               | ((timestamp - TWEPOCH) << TIMESTAMP_SHIFT)
               | sequence;
    }

}
