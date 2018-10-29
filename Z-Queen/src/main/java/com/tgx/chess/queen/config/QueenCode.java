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

package com.tgx.chess.queen.config;

import com.tgx.chess.king.config.KingCode;

public interface QueenCode
        extends
        KingCode
{
    /**
     * ClientManager
     * Accept devices' connection request
     */
    long CM_XID                               = 0x8000000000000000L;
    /**
     * Cluster Manager
     * symmetry communication
     */
    long RM_XID                               = 0xC000000000000000L;
    /**
     * Internal system message queue
     */
    long MQ_XID                               = 0x4000000000000000L;
    /**
     * device connection
     */
    long CU_XID                               = 0x0000000000000000L;

    long XID_MK                               = 0xC000000000000000L;
    long XID_N_MK                             = 0x3FFF000000000000L;
    long STP_MK                               = 0xFFFF000000000000L;
    int  XID_N_MK_S                           = 0x00003FFF;
    int  XID_MK_S                             = 0x0000C000;
    long UID_TIME_27_MK                       = ((1L << 27) - 1) << 21;
    long UID_SEQ_21_MK                        = (1L << 21) - 1;
    long UID_TIME_28_MK                       = ((1L << 28) - 1) << 20;
    long UID_SEQ_20_MK                        = (1L << 20) - 1;
    long UID_SEQ_20_1_MK                      = ((1L << 20) - 1) << 1;
    long UID_SEQ_18_3_MK                      = ((1L << 18) - 1) << 3;
    long UID_SEQ_17_4_MK                      = ((1L << 17) - 1) << 4;

    long UID_MK                               = 0x0000FFFFFFFFFFFFL;

    long _IndexMask                           = 0x0000FFFFFFFFFFFFL;
    long _IndexHighMask                       = 0xFFFF000000000000L;
    long _IndexPortMask                       = 0xFFFFFFFFFFFF0000L;
    long _UsrIndexMask                        = 0x0000FFFFFFFFFFFEL;

    /* HA address max size 256 */
    int  _HA_ROUTER_REMOTE_ADDRESS_INDEX_MASK = 0xFF;
    int  DEAULT_USR_BIND_SIZE                 = 3;

    int  EXIT_CODE_NO_ARGUMENT                = 404;
    int  EXIT_CODE_START_FAILED               = 900;

    int  UNKNOWN                              = -1;
    int  PLAIN_UNSUPPORTED                    = 103;
    int  PLAIN_VERSION_LOWER                  = 104;
    int  SYMMETRIC_KEY_OK                     = 110;
    int  SYMMETRIC_KEY_REROLL                 = 111;
    int  DEVICE_OK                            = 300;
    int  DEVICE_AUTHORING_KEY_ERROR           = 301;
    int  DEVICE_AUTHORING_KEY_OUT_OF_DATE     = 302;
    int  DEVICE_DUPLICATE                     = 303;
    int  DEVICE_FORBIDDEN                     = 304;
    int  DEVICE_NOT_FOUND                     = 305;
    int  USR_OK                               = 400;
    int  USR_FORBIDDEN                        = 401;
    int  USR_NOT_FOUND                        = 402;
    int  USR_AUTHORING_KEY_ERROR              = 403;
    int  USR_AUTHORING_KEY_OUT_OF_DATE        = 404;
    int  USR_DUPLICATE                        = 405;
    int  USR_FAILED                           = 406;
    int  USR_DELETE                           = 407;
    int  SERVICE_ERROR                        = 502;
    int  DEVICE_CLUSTER_ERROR                 = 503;
    int  ROUTER_CLUSTER_ERROR                 = 504;
    int  MQ_REGISTER_TOPIC_OK                 = 600;
    int  MQ_REGISTER_TOPIC_DUPLICATE          = 601;
    int  MQ_REGISTER_TOPIC_MODE_CONFLICT      = 602;
    int  MQ_REGISTER_TOPIC_NULL               = 603;

    public static String parseRCode(int code)
    {
        switch (code)
        {
            case DEVICE_OK:
                return "DEVICE_OK";
            case USR_OK:
                return "USR_OK";
            case SYMMETRIC_KEY_OK:
                return "SYMMETRIC_KEY_OK";
            case SYMMETRIC_KEY_REROLL:
                return "SYMMETRIC_KEY_REROLL";
            case DEVICE_AUTHORING_KEY_ERROR:
                return "DEVICE_AUTHORING_KEY_ERROR";
            case DEVICE_AUTHORING_KEY_OUT_OF_DATE:
                return "DEVICE_AUTHORING_KEY_OUT_OF_DATE";
            case DEVICE_DUPLICATE:
                return "DEVICE_DUPLICATE";
            case DEVICE_FORBIDDEN:
                return "DEVICE_FORBIDDEN";
            case DEVICE_NOT_FOUND:
                return "DEVICE_NOT_FOUND";
            case USR_FORBIDDEN:
                return "USR_FORBIDDEN";
            case USR_NOT_FOUND:
                return "USR_NOT_FOUND";
            case USR_AUTHORING_KEY_ERROR:
                return "USR_AUTHORING_KEY_ERROR";
            case USR_AUTHORING_KEY_OUT_OF_DATE:
                return "USR_AUTHORING_KEY_OUT_OF_DATE";
            case USR_DUPLICATE:
                return "USR_DUPLICATE";
            case USR_FAILED:
                return "USR_FAILED";
            case USR_DELETE:
                return "USR_DELETE";
            case SERVICE_ERROR:
                return "SERVICE_ERROR";
            case DEVICE_CLUSTER_ERROR:
                return "DEVICE_CLUSTER_ERROR";
            case ROUTER_CLUSTER_ERROR:
                return "ROUTER_CLUSTER_ERROR";
            case MQ_REGISTER_TOPIC_OK:
                return "MQ_REGISTER_TOPIC_OK";
            case MQ_REGISTER_TOPIC_DUPLICATE:
                return "MQ_REGISTER_TOPIC_DUPLICATE";
            case MQ_REGISTER_TOPIC_MODE_CONFLICT:
                return "MQ_REGISTER_TOPIC_MODE_CONFLICT";
            case MQ_REGISTER_TOPIC_NULL:
                return "MQ_REGISTER_TOPIC_NULL";
            default:
                return "UNKNOWN";
        }
    }
}
