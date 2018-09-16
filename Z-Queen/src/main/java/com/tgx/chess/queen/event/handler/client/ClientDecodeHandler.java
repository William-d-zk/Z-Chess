package com.tgx.chess.queen.event.handler.client;

import static com.tgx.chess.queen.event.inf.IOperator.Type.LOGIC;

import com.tgx.chess.queen.event.handler.DecodeHandler;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public class ClientDecodeHandler
        extends
        DecodeHandler
{

    @Override
    protected void transfer(QEvent event, ICommand[] commands, ISession session, IOperator<ICommand[], ISession> operator) {
        event.produce(LOGIC, commands, session, operator);
    }
}
