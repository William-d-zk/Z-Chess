package com.isahl.chess.board.processor;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author william.d.zk
 */
@SupportedAnnotationTypes({ "com.isahl.chess.board.annotation.ISerialGenerator",
                            "com.isahl.chess.board.annotation.ISerialFactory"
})
public class ZAnnotationProcessor
        extends AbstractProcessor
{
    protected Filer                      mFiler;
    protected Elements                   mElementUtils;
    protected JavacProcessingEnvironment mEnvironment;
    protected Writer                     mWriter;

    private final List<IAnnotationProcessor> _ProcessorQueue = new LinkedList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        mEnvironment = (JavacProcessingEnvironment) processingEnv;
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        note(format("custom processor: %s", getClass().getSimpleName()));
        addProcessor(new SerialProcessor(this));
        addProcessor(new FactoryProcessor(this));
    }

    public ZAnnotationProcessor()
    {
        try {
            String path = System.getProperty("user.home") + File.separator + "Z-Chess" + File.separator + "processor";
            File directory = new File(path);
            File sf;
            if(!directory.exists() && !directory.mkdirs()) {
                return;
            }
            sf = new File(path + File.separator + "annotation.log");
            if(!sf.exists() && !sf.createNewFile()) {
                return;
            }
            mWriter = new PrintWriter(sf);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        for(IAnnotationProcessor processor : _ProcessorQueue) {
            if(processor.process(annotations, roundEnv)) {
                break;
            }
        }
        return true;
    }

    protected void note(String msg)
    {
        try {
            mWriter.write(format("%s\n", msg));
            mWriter.flush();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void addProcessor(IAnnotationProcessor processor)
    {
        _ProcessorQueue.add(processor);
    }
}
