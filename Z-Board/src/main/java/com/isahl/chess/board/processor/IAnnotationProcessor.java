package com.isahl.chess.board.processor;

import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * @author william.d.zk
 */
public interface IAnnotationProcessor {
  boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);
}
