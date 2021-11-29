package com.isahl.chess.board.processor;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.sun.source.util.Trees;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 */
public class FactoryProcessor
        implements IAnnotationProcessor
{
    private final ZAnnotationProcessor _ZProcessor;

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
            final Map<String, Pair<Integer, Integer>> sourceMap = new HashMap<>();

            roundEnv.getElementsAnnotatedWith(ISerialGenerator.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        _ZProcessor.note("serial class: %s", className);
                        ISerialGenerator generator = element.getAnnotation(ISerialGenerator.class);
                        int parent = generator.parent();
                        int serial = generator.serial();
                        sourceMap.put(className, Pair.of(parent, serial));
                    });
            Map<Integer, List<Pair<String, Integer>>> factoryMap = new HashMap<>();
            roundEnv.getElementsAnnotatedWith(ISerialFactory.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        _ZProcessor.note("factory class:%s", className);
                        ISerialFactory factory = element.getAnnotation(ISerialFactory.class);
                        int parent = factory.parent();
                        factoryMap.put(parent,
                                       sourceMap.entrySet()
                                                .stream()
                                                .filter(entry->entry.getValue().fst == parent)
                                                .map(entry->Pair.of(entry.getKey(), entry.getValue().snd))
                                                .collect(Collectors.toList()));

                    });

        }
        return false;
    }
}
