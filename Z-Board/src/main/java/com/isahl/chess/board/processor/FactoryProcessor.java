package com.isahl.chess.board.processor;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
            final Map<String, Pair<Integer, Integer>> _SourceMap = new HashMap<>();
            final Map<Integer, List<Pair<String, Integer>>> _FactoryMap = new HashMap<>();
            note("factory processor");
            RandomAccessFile globalFile;
            try {
                String path =
                        System.getProperty("user.home") + File.separator + "Z-Chess" + File.separator + "processor";
                File directory = new File(path);
                File sf;
                if(!directory.exists() && !directory.mkdirs()) {
                    return false;
                }
                sf = new File(path + File.separator + "global.annotation");
                if(!sf.exists() && !sf.createNewFile()) {
                    return false;
                }
                globalFile = new RandomAccessFile(sf, "r");
                if(globalFile.length() > 0) {
                    String line;
                    Pattern classPattern = Pattern.compile("(.+)\\[(\\d+)]@<(\\d+)>");
                    while((line = globalFile.readLine()) != null) {
                        Matcher matcher = classPattern.matcher(line);
                        if(matcher.matches()) {
                            String className = matcher.group(1);
                            int serial = Integer.parseInt(matcher.group(2));
                            int parent = Integer.parseInt(matcher.group(3));
                            _SourceMap.put(className, Pair.of(parent, serial));
                        }
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();

                return false;
            }

            roundEnv.getElementsAnnotatedWith(ISerialFactory.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        note(format("factory class:%s", className));
                        ISerialFactory factory = element.getAnnotation(ISerialFactory.class);
                        int parent = factory.parent();
                        _FactoryMap.put(parent,
                                        _SourceMap.entrySet()
                                                  .stream()
                                                  .filter(entry->entry.getValue().fst == parent)
                                                  .map(entry->Pair.of(entry.getKey(), entry.getValue().snd))
                                                  .collect(Collectors.toList()));
                        TreePath treePath = _Trees.getPath(element);
                        JCTree.JCCompilationUnit unit = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
                        JCTree tree = unit.getTree();
                        tree.accept(new FactoryTranslator(this, _Context, _FactoryMap.get(parent)));
                    });

        }
        return false;
    }

    public void note(String msg)
    {
        _ZProcessor.note(msg);
    }
}
