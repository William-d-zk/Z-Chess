/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.board.processor;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.processor.model.ProcessorContext;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author william.d.zk
 */
public class SerialProcessor
        implements IAnnotationProcessor
{

    private final ZAnnotationProcessor _ZProcessor;
    private final Logger               _Logger = LoggerFactory.getLogger("base.board." + getClass().getSimpleName());

    public SerialProcessor(ZAnnotationProcessor zProcessor) {_ZProcessor = zProcessor;}

    private boolean mHasMethodSerial, mHasMethodSuper, mHasFieldSerial;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if(!roundEnv.processingOver()) {
            final Context _Context = _ZProcessor.mEnvironment.getContext();
            final Trees _Trees = Trees.instance(_ZProcessor.mEnvironment);
            final ProcessorContext _ProcessorContext = _ZProcessor.getProcessorContext();

            roundEnv.getElementsAnnotatedWith(ISerialGenerator.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        _Logger.debug(format("annotation class: %s", className));
                        List<? extends Element> enclosedElements = element.getEnclosedElements();
                        for(Element enclosed : enclosedElements) {
                            if(enclosed.getKind() == ElementKind.METHOD) {
                                switch(enclosed.getSimpleName()
                                               .toString()) {
                                    case "serial" -> mHasMethodSerial = true;
                                    case "_super" -> mHasMethodSuper = true;
                                }
                            }
                            if(enclosed.getKind() == ElementKind.FIELD) {
                                switch(enclosed.getSimpleName()
                                               .toString()) {
                                    case "_SERIAL" -> mHasFieldSerial = true;
                                }
                            }
                        }
                        ISerialGenerator generator = element.getAnnotation(ISerialGenerator.class);
                        final int parent = generator.parent();
                        final int serial = generator.serial();

                        //update serial-field-count key 必然存在
                        _ProcessorContext.parentIncrementCount(parent);

                        JCTree tree = (JCTree) _Trees.getTree(element);
                        tree.accept(new SerialTranslator(this,
                                                         _Context,
                                                         generator.parent(),
                                                         _ProcessorContext.onSerialAnnotation(className,
                                                                                              serial,
                                                                                              parent)));
                    });
            try {
                _ProcessorContext.update();
                _Logger.info("process context updated");
            }
            catch(IOException e) {
                _Logger.warn("processor context update ", e);
            }
        }
        return false;
    }

    public boolean existMethodSuper()
    {
        return mHasMethodSuper;
    }

    public boolean existMethodSerial()
    {
        return mHasMethodSerial;
    }

    public boolean existFieldSerial()
    {
        return mHasFieldSerial;
    }
}
