package com.tgx.z.queen.config;

import com.tgx.z.king.config.ConfigKey;

public interface QueenConfigKey
        extends
        ConfigKey
{

    String OWNER_QUEEN_POWER      = "power";
    String KEY_POWER_SERVER       = "server";
    String KEY_POWER_CLUSTER      = "cluster";
    String KEY_POWER_INTERNAL     = "internal";
    String KEY_POWER_CLIENT       = "client";
    String KEY_POWER_LINK         = "link";
    String KEY_POWER_ERROR        = "error";
    String KEY_POWER_LOGIC        = "logic";

    String OWNER_PIPELINE_CORE    = "core";
    String KEY_CORE_SERVER        = "server";
    String KEY_CORE_CLUSTER       = "cluster";
    String KEY_CORE_DECODER       = "decoder";
    String KEY_CORE_LOGIC         = "logic";
    String KEY_CORE_ENCODER       = "encoder";

    String OWNER_SOCKET_OPTION    = "option";
    String KEY_OPTION_SNF         = "snf";
    String KEY_OPTION_RCV         = "rcv";
    String KEY_OPTION_MTU         = "mtu";
    String KEY_OPTION_KEEP_ALIVE  = "keep_alive";
    String KEY_OPTION_TCP_NODELAY = "tcp_nodelay";
    String KEY_OPTION_SO_LINGER   = "so_linger";

    String OWNER_SOCKET_SEND      = "send";
    String KEY_SEND_QUEUE_SIZE    = "queue_size";
    String OWNER_SOCKET_IN        = "in";
    String KEY_IN_MINUTE          = "minute";
    String OWNER_SOCKET_OUT       = "out";
    String KEY_OUT_SECOND         = "second";
}
