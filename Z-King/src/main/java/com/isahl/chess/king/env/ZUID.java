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

package com.isahl.chess.king.env;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2^64 unsigned long data encoding
 * session port-prefix max 2^16
 * 00-0000-000-0000000-0000000000000000000000000000000000000-000000000
 * [02bit] peer-type
 * <p>
 *     {00} Client manager service
 *     {01} Internal message queue broker
 *     {10} Device consumer connection
 *     {11} Cluster symmetry communication
 * </p>
 * [04bit] cluster-region
 * [03bit] cluster-set identity
 * [07bit] cluster-node identity
 * [38bit] timestamp gap 2021-06-01 00:00:00.000
 * [10bit] sequence in one millisecond
 */
public class ZUID
{

    public static final LocalDateTime EPOCH_DATE         = LocalDateTime.of(2021, Month.JUNE, 1, 0, 0);
    public static final long          EPOCH_SECOND       = EPOCH_DATE.toEpochSecond(ZoneOffset.UTC);
    public static final int           SEQUENCE_BITS      = 10;
    public static final long          SEQUENCE_MASK      = ~(-1L << SEQUENCE_BITS);
    public static final int           TIMESTAMP_BITS     = 38;
    public static final int           TIMESTAMP_SHIFT    = SEQUENCE_BITS;
    public static final int           NODE_BITS          = 7;
    public static final int           NODE_SHIFT         = TIMESTAMP_SHIFT + TIMESTAMP_BITS;
    public static final int           CLUSTER_BITS       = 3;
    public static final int           CLUSTER_SHIFT      = NODE_SHIFT + NODE_BITS;
    public static final int           IDC_BITS           = 4;
    public static final int           IDC_SHIFT          = CLUSTER_SHIFT + CLUSTER_BITS;
    public static final int           TYPE_BITS          = 2;
    public static final int           TYPE_SHIFT         = IDC_SHIFT + IDC_BITS;
    public static final int           MAX_TYPE           = (1 << TYPE_BITS) - 1;
    public static final int           MAX_IDC_ID         = (1 << IDC_BITS) - 1;
    public static final int           MAX_CLUSTER_SET_ID = (1 << CLUSTER_BITS) - 1;
    public static final int           MAX_NODE_ID        = (1 << NODE_BITS) - 1;

    private static final String         UNAME_FORMATTER    = "%d_%d_%d_%d@%d";
    private static final Pattern        UNAME_PATTERN      = Pattern.compile("(\\d+)_(\\d+)_(\\d+)_(\\d+)@(\\d+)");
    /* ==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--= */
    public static final  long           TYPE_MASK          = ((1L << TYPE_BITS) - 1) << TYPE_SHIFT;
    public static final  long           TYPE_CONSUMER      = 0;
    public static final  long           TYPE_INTERNAL      = 1L << TYPE_SHIFT;
    public static final  long           TYPE_PROVIDER      = 2L << TYPE_SHIFT;
    public static final  long           TYPE_CLUSTER       = 3L << TYPE_SHIFT;
    public static final  long           CLUSTER_MASK       = (((1L << IDC_BITS) - 1) << IDC_SHIFT) |
                                                             (((1L << CLUSTER_BITS) - 1) << CLUSTER_SHIFT);
    public static final  long           NODE_MASK          = ((1L << NODE_BITS) - 1) << NODE_SHIFT;
    public static final  long           PEER_MASK          = CLUSTER_MASK | NODE_MASK;
    public static final  long           PREFIX_MASK        = -1L << 48;
    public static final  int            TYPE_CONSUMER_SLOT = 0;
    public static final  int            TYPE_INTERNAL_SLOT = 1;
    public static final  int            TYPE_PROVIDER_SLOT = 2;
    public static final  int            TYPE_CLUSTER_SLOT  = 3;
    /* ==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--= */
    private final        long           _IdcId;
    private final        long           _ClusterId;
    private final        long           _NodeId;
    private final        long           _Type;
    private final        Supplier<Long> _TimestampSupplier;
    private              long           mSequence;
    private              long           mLastTimestamp;

    public ZUID(long idc_id, long cluster_id, long node_id, long type)
    {
        if(idc_id > MAX_IDC_ID || idc_id < 0) {
            throw new IllegalArgumentException(String.format("idc region Id can't be greater than %d or less than 0",
                                                             MAX_IDC_ID));
        }
        if(cluster_id > MAX_CLUSTER_SET_ID || cluster_id < 0) {
            throw new IllegalArgumentException(String.format("cluster Id can't be greater than %d or less than 0",
                                                             MAX_CLUSTER_SET_ID));
        }
        if(node_id > MAX_NODE_ID || node_id < 0) {
            throw new IllegalArgumentException(String.format("node Id can't be greater than %d or less than 0",
                                                             MAX_NODE_ID));
        }
        if(type > MAX_TYPE || type < 0) {
            throw new IllegalArgumentException(String.format("type can't be greater than %d or less than 0", MAX_TYPE));
        }
        _IdcId = idc_id;
        _ClusterId = cluster_id;
        _NodeId = node_id;
        _Type = type;
        _TimestampSupplier = System::currentTimeMillis;
    }

    public ZUID()
    {
        this(0, 0, 0, 0);
    }

    public ZUID(String uname)
    {
        Matcher matcher = UNAME_PATTERN.matcher(uname);
        if(matcher.matches()) {
            long idc_id = Long.parseLong(matcher.group(1));
            long cluster_id = Long.parseLong(matcher.group(2));
            long node_id = Long.parseLong(matcher.group(3));
            long type = Long.parseLong(matcher.group(4));
            if(idc_id > MAX_IDC_ID || idc_id < 0) {
                throw new IllegalArgumentException(String.format("idc region Id can't be greater than %d or less than 0",
                                                                 MAX_IDC_ID));
            }
            if(cluster_id > MAX_CLUSTER_SET_ID || cluster_id < 0) {
                throw new IllegalArgumentException(String.format("cluster Id can't be greater than %d or less than 0",
                                                                 MAX_CLUSTER_SET_ID));
            }
            if(node_id > MAX_NODE_ID || node_id < 0) {
                throw new IllegalArgumentException(String.format("node Id can't be greater than %d or less than 0",
                                                                 MAX_NODE_ID));
            }
            if(type > MAX_TYPE || type < 0) {
                throw new IllegalArgumentException(String.format("type can't be greater than %d or less than 0",
                                                                 MAX_TYPE));
            }
            _IdcId = idc_id;
            _ClusterId = cluster_id;
            _NodeId = node_id;
            _Type = type;
        }
        else {
            _IdcId = 0;
            _ClusterId = 0;
            _NodeId = 0;
            _Type = 0;
        }
        _TimestampSupplier = System::currentTimeMillis;
    }

    public synchronized long getId()
    {
        return getId(_Type << TYPE_SHIFT);
    }

    public String getName()
    {
        return String.format(UNAME_FORMATTER, _Type, _IdcId, _ClusterId, _NodeId, _TimestampSupplier.get());
    }

    public long getId(long type)
    {
        type &= TYPE_MASK;
        generateSequence();
        return (_IdcId << IDC_SHIFT) | (_ClusterId << CLUSTER_SHIFT) | (_NodeId << NODE_SHIFT) |
               ((mLastTimestamp - EPOCH_SECOND) << TIMESTAMP_SHIFT) | type | mSequence;
    }

    public long moveOn(long id)
    {
        long prefix = PREFIX_MASK & id;
        generateSequence();
        return ((mLastTimestamp << TIMESTAMP_SHIFT) | mSequence) & ~PREFIX_MASK | prefix;
    }

    private void generateSequence()
    {
        long timestamp = _TimestampSupplier.get();
        if(mLastTimestamp == timestamp) {
            mSequence = (mSequence + 1) & SEQUENCE_MASK;
            if(mSequence == 0) {
                LockSupport.parkUntil(++timestamp);
            }
        }
        else {
            mSequence = 0L;
        }
        mLastTimestamp = timestamp;
    }

    public long getPeerId()
    {
        return (_IdcId << IDC_SHIFT) | (_ClusterId << CLUSTER_SHIFT) | (_NodeId << NODE_SHIFT) | (_Type << TYPE_SHIFT);
    }

    public long getPeerIdByNode(long nodeId)
    {
        nodeId &= (1L << NODE_BITS) - 1;
        return (_IdcId << IDC_SHIFT) | (_ClusterId << CLUSTER_SHIFT) | (nodeId << NODE_SHIFT) | (_Type << TYPE_SHIFT);
    }

    public boolean isTheGate(long id)
    {
        return (id & CLUSTER_MASK) != _ClusterId && _NodeId == 0;
    }

    public final static long INVALID_PEER_ID = 0;

    @Override
    public String toString()
    {
        return "ZUID{" + "IdcId=" + _IdcId + ", ClusterId=" + _ClusterId + ", NodeId=" + _NodeId + ", Type=" + _Type +
               ", sequence=" + mSequence + '}';
    }

    public static boolean isTypeOfCluster(long id)
    {
        return (id & TYPE_MASK) == TYPE_CLUSTER;
    }

    public static int getNodeId(long id)
    {
        return (int) ((id & NODE_MASK) >>> NODE_SHIFT);
    }

}
