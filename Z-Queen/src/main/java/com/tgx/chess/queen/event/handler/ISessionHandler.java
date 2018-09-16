package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.io.core.inf.IContext.ENCODE_ERROR;

import java.util.Objects;

import com.lmax.disruptor.EventHandler;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IError.Type;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

public interface ISessionHandler
        extends
        EventHandler<QEvent>
{
    default <A> void encodeHandler(QEvent event, A a, ISession session, IOperator<A, ISession> operator) {
        IContext context = session.getContext();
        if (!context.isOutErrorState()) {
            Triple<Throwable, ISession, IOperator<Throwable, ISession>> result = operator.handle(a, session);
            if (Objects.nonNull(result)) {
                event.error(Type.FILTER_DECODE, result.first(), result.second(), result.third());
                context.setOutState(ENCODE_ERROR);
            }
            else {
                event.ignore();
            }
        }
    }
}
