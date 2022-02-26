package com.isahl.chess.board.processor;

import com.isahl.chess.board.processor.model.ProcessorContext;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author william.d.zk
 */
@SupportedAnnotationTypes({ "com.isahl.chess.board.annotation.ISerialGenerator",
                            "com.isahl.chess.board.annotation.ISerialFactory" })
public class ZAnnotationProcessor
        extends AbstractProcessor
{
    protected Elements                   mElementUtils;
    protected JavacProcessingEnvironment mEnvironment;
    protected ProcessorContext           mContext = new ProcessorContext();

    private final List<IAnnotationProcessor> _ProcessorQueue = new LinkedList<>();
    private final Logger                     _Logger         = LoggerFactory.getLogger(
            "base.board." + getClass().getSimpleName());

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        mEnvironment = (JavacProcessingEnvironment) processingEnv;
        mElementUtils = processingEnv.getElementUtils();
        _Logger.debug("init processor: ----------------------------------------------------------------");
        try {
            mContext.init();
        }
        catch(IOException e) {
            _Logger.warn("processor context initialize failed!", e);
            throw new IllegalStateException("stop annotation process");
        }
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
        return true;
    }

    public void addProcessor(IAnnotationProcessor processor) {_ProcessorQueue.add(processor);}

    public ProcessorContext getProcessorContext() {return mContext;}
}
