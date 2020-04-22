package com.tgx.chess.queen.event.handler.cluster;

import com.lmax.disruptor.EventHandler;
import com.tgx.chess.queen.event.processor.QEvent;

public class NotifyHandler
        implements
        EventHandler<QEvent>
{
    @Override
    public void onEvent(QEvent event, long seq, boolean batch) throws Exception
    {

    }
}
