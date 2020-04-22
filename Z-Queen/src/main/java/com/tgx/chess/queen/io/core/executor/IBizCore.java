package com.tgx.chess.queen.io.core.executor;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;

/**
 * @author william.d.zk
 */
public interface IBizCore
        extends
        IPipeCore
{
    AsynchronousChannelGroup getServiceChannelGroup() throws IOException;
}
