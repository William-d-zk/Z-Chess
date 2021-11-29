package com.isahl.chess.board.processor;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author william.d.zk
 */
@SupportedAnnotationTypes({
        "com.isahl.chess.board.annotation.ISerialGenerator",
        "com.isahl.chess.board.annotation.ISerialFactory"
})
public class ZAnnotationProcessor
        extends AbstractProcessor
{
    protected Filer                      mFiler;
    protected Messager                   mMessager;
    protected Elements                   mElementUtils;
    protected JavacProcessingEnvironment mEnvironment;

    private final List<IAnnotationProcessor> _ProcessorQueue = new LinkedList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        mEnvironment = (JavacProcessingEnvironment) processingEnv;
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
        note("custom processor: %s", getClass().getSimpleName());
        addProcessor(new SerialProcessor(this));
        addProcessor(new FactoryProcessor(this));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        for(IAnnotationProcessor processor : _ProcessorQueue) {
            if(processor.process(annotations, roundEnv)) {
                break;
            }
        }
        return false;
    }

    protected void note(String msg)
    {
        mMessager.printMessage(Diagnostic.Kind.NOTE, msg);
        System.out.println(msg);
    }

    protected void note(String format, Object... args)
    {
        mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args));
        System.out.println(String.format(format, args));
    }

    protected void write(Writer writer, StringBuilder builder, String... contents) throws IOException
    {
        for(String str : contents) {
            writer.write(str);
            builder.append(str);
        }
    }

    public void addProcessor(IAnnotationProcessor processor)
    {
        _ProcessorQueue.add(processor);
    }
}
