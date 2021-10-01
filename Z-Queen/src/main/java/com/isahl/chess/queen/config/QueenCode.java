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

package com.isahl.chess.queen.config;

import com.isahl.chess.king.config.KingCode;

/**
 * @author william.d.zk
 */
public interface QueenCode
        extends KingCode
{

    /**
     * HA address max size 256
     */
    int _HA_ROUTER_REMOTE_ADDRESS_INDEX_MASK = 0xFF;
    int DEFAULT_USR_BIND_SIZE                = 3;

    int EXIT_CODE_NO_ARGUMENT  = 404;
    int EXIT_CODE_START_FAILED = 900;

    int DEVICE_OK                        = 0x00200;
    int DEVICE_AUTHORING_KEY_ERROR       = 0x00301;
    int DEVICE_AUTHORING_KEY_OUT_OF_DATE = 0x00302;
    int DEVICE_DUPLICATE                 = 0x00303;
    int DEVICE_FORBIDDEN                 = 0x00304;
    int DEVICE_NOT_FOUND                 = 0x00305;
    int USR_OK                           = 0x00400;
    int USR_FORBIDDEN                    = 0x00401;
    int USR_NOT_FOUND                    = 0x00402;
    int USR_AUTHORING_KEY_ERROR          = 0x00403;
    int USR_AUTHORING_KEY_OUT_OF_DATE    = 0x00404;
    int USR_DUPLICATE                    = 0x00405;
    int USR_FAILED                       = 0x00406;
    int USR_DELETE                       = 0x00407;
    int SERVICE_ERROR                    = 0x00502;
    int DEVICE_CLUSTER_ERROR             = 0x00503;
    int ROUTER_CLUSTER_ERROR             = 0x00504;
    int MQ_REGISTER_TOPIC_OK             = 0x00600;
    int MQ_REGISTER_TOPIC_DUPLICATE      = 0x00601;
    int MQ_REGISTER_TOPIC_MODE_CONFLICT  = 0x00602;
    int MQ_REGISTER_TOPIC_NULL           = 0x00603;

    String ERROR_CLOSE = "error close";
    String LOCAL_CLOSE = "error close";

    static String codeOf(int code)
    {
        return switch(code) {
            case DEVICE_OK -> "DEVICE_OK";
            case USR_OK -> "USR_OK";
            case DEVICE_AUTHORING_KEY_ERROR -> "DEVICE_AUTHORING_KEY_ERROR";
            case DEVICE_AUTHORING_KEY_OUT_OF_DATE -> "DEVICE_AUTHORING_KEY_OUT_OF_DATE";
            case DEVICE_DUPLICATE -> "DEVICE_DUPLICATE";
            case DEVICE_FORBIDDEN -> "DEVICE_FORBIDDEN";
            case DEVICE_NOT_FOUND -> "DEVICE_NOT_FOUND";
            case USR_FORBIDDEN -> "USR_FORBIDDEN";
            case USR_NOT_FOUND -> "USR_NOT_FOUND";
            case USR_AUTHORING_KEY_ERROR -> "USR_AUTHORING_KEY_ERROR";
            case USR_AUTHORING_KEY_OUT_OF_DATE -> "USR_AUTHORING_KEY_OUT_OF_DATE";
            case USR_DUPLICATE -> "USR_DUPLICATE";
            case USR_FAILED -> "USR_FAILED";
            case USR_DELETE -> "USR_DELETE";
            case SERVICE_ERROR -> "SERVICE_ERROR";
            case DEVICE_CLUSTER_ERROR -> "DEVICE_CLUSTER_ERROR";
            case ROUTER_CLUSTER_ERROR -> "ROUTER_CLUSTER_ERROR";
            case MQ_REGISTER_TOPIC_OK -> "MQ_REGISTER_TOPIC_OK";
            case MQ_REGISTER_TOPIC_DUPLICATE -> "MQ_REGISTER_TOPIC_DUPLICATE";
            case MQ_REGISTER_TOPIC_MODE_CONFLICT -> "MQ_REGISTER_TOPIC_MODE_CONFLICT";
            case MQ_REGISTER_TOPIC_NULL -> "MQ_REGISTER_TOPIC_NULL";
            default -> KingCode.codeOf(code);
        };
    }
}
