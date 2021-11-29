package com.isahl.chess.board.processor;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author william.d.zk
 */
public interface IAnnotationProcessor
{
    boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);
}
