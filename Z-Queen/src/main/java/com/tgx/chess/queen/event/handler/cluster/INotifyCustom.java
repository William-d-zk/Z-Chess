package com.tgx.chess.queen.event.handler.cluster;

import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 */
public interface INotifyCustom
{
    void notify(IProtocol request);
}
