package com.isahl.chess.board.processor;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.List;
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

    private boolean mHasMethodSerial, mHasMethodSupport, mHasFieldSerial;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if(!roundEnv.processingOver()) {
            final Context _Context = _ZProcessor.mEnvironment.getContext();
            final Trees _Trees = Trees.instance(_ZProcessor.mEnvironment);
            _Logger.info("factory processor ==============================================================");
            roundEnv.getElementsAnnotatedWith(ISerialFactory.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        _Logger.info(format("factory class:%s", className));
                        ISerialFactory factory = element.getAnnotation(ISerialFactory.class);
                        int serial = factory.serial();
                        List<? extends Element> enclosedElements = element.getEnclosedElements();
                        for(Element enclosed : enclosedElements) {
                            if(enclosed.getKind() == ElementKind.METHOD) {
                                switch(enclosed.getSimpleName()
                                               .toString()) {
                                    case "serial" -> mHasMethodSerial = true;
                                    case "isSupport" -> mHasMethodSupport = true;
                                }
                            }
                            if(enclosed.getKind() == ElementKind.FIELD) {
                                if("_SERIAL".equals(enclosed.getSimpleName()
                                                            .toString()))
                                {
                                    mHasFieldSerial = true;
                                }
                            }
                        }
                        JCTree tree = (JCTree) _Trees.getTree(element);
                        tree.accept(new FactoryTranslator(this, _Context, serial));
                    });

        }
        return false;
    }

    public boolean existMethodSerial()
    {
        return mHasMethodSerial;
    }

    public boolean existFieldSerial()
    {
        return mHasFieldSerial;
    }

    private void createSwitchBuild(Trees trees, Element element, Context context, int parent)
    {
        TreePath treePath = trees.getPath(element);
        JCTree.JCCompilationUnit unit = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
        JCTree tree = unit.getTree();
        tree.accept(new SwitchBuilderTranslator(context,
                                                _ZProcessor.getProcessorContext()
                                                           .getChildren(parent)));
    }
}
