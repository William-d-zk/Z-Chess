package com.tgx.z.queen.event.handler.client;

import static com.tgx.z.queen.event.inf.IOperator.Type.LOGIC;

import com.tgx.z.queen.event.handler.DecodeHandler;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.ISession;

public class ClientDecodeHandler
        extends
        DecodeHandler
{

    @Override
    protected void transfer(QEvent event, ICommand[] commands, ISession session, IOperator<ICommand[], ISession> operator) {
        event.produce(LOGIC, commands, session, operator);
    }
}
