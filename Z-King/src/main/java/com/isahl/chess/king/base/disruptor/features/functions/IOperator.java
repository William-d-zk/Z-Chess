package com.isahl.chess.king.base.disruptor.features.functions;

import java.util.Objects;

public interface IOperator<T, R>
{
    R handle(T t);
    default <V> IOperator<T, V> andThen(IOperator<? super R, ? extends V> after)
    {
        Objects.requireNonNull(after);
        return (T t)->after.handle(handle(t));
    }

    default String getName()
    {
        return "operator.";
    }
}
