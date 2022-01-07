package com.isahl.chess.board.processor;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author william.d.zk
 */
public class FactoryProcessor
        implements IAnnotationProcessor
{
    private final ZAnnotationProcessor _ZProcessor;
    private final Logger               _Logger = LoggerFactory.getLogger("base.board." + getClass().getSimpleName());

    public FactoryProcessor(ZAnnotationProcessor zProcessor)
    {
        _ZProcessor = zProcessor;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if(!roundEnv.processingOver()) {
            final Context _Context = _ZProcessor.mEnvironment.getContext();
            final Trees _Trees = Trees.instance(_ZProcessor.mEnvironment);
            _Logger.info("factory processor");
            roundEnv.getElementsAnnotatedWith(ISerialFactory.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        _Logger.info(format("factory class:%s", className));
                        ISerialFactory factory = element.getAnnotation(ISerialFactory.class);
                        int parent = factory.parent();

                        TreePath treePath = _Trees.getPath(element);
                        JCTree.JCCompilationUnit unit = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
                        JCTree tree = unit.getTree();
                        tree.accept(new FactoryTranslator(_Context,
                                                          _ZProcessor.getProcessorContext()
                                                                     .getChildren(parent)));
                    });

        }
        return false;
    }

}
