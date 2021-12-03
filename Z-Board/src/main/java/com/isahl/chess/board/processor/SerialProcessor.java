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
import com.isahl.chess.board.base.ISerial;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * @author william.d.zk
 */
public class SerialProcessor
        implements IAnnotationProcessor
{

    private final ZAnnotationProcessor _ZProcessor;

    public SerialProcessor(ZAnnotationProcessor zProcessor) {_ZProcessor = zProcessor;}

    private boolean mHasMethodSerial, mHasMethodSuper, mHasFieldSerial;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if(!roundEnv.processingOver()) {
            final Context _Context = _ZProcessor.mEnvironment.getContext();
            final Trees _Trees = Trees.instance(_ZProcessor.mEnvironment);

            Map<String, Pair<Integer, Integer>> classMap = new TreeMap<>();
            Map<Integer, Pair<String, Integer>> parentCountMap = new TreeMap<>();
            Set<Integer> serialSet = new TreeSet<>();
            try {
                for(Field field : ISerial.class.getFields()) {
                    int parent = field.getInt(ISerial.class);
                    String name = field.getName();
                    parentCountMap.put(parent, Pair.of(name, 0));
                }
            }
            catch(IllegalAccessException e) {
                e.printStackTrace();
            }

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
                    Pattern fieldPattern = Pattern.compile("(\\w+):(\\d+)=(\\d+)");
                    Pattern classPattern = Pattern.compile("(.+)\\[(\\d+)]@<(\\d+)>");
                    while((line = globalFile.readLine()) != null) {
                        Matcher matcher = fieldPattern.matcher(line);
                        if(matcher.matches()) {
                            String field = matcher.group(1);
                            int parent = Integer.parseInt(matcher.group(2));
                            int count = Integer.parseInt(matcher.group(3));
                            parentCountMap.put(parent, Pair.of(field, count));
                        }
                        matcher = classPattern.matcher(line);
                        if(matcher.matches()) {
                            String className = matcher.group(1);
                            int serial = Integer.parseInt(matcher.group(2));
                            int parent = Integer.parseInt(matcher.group(3));
                            classMap.put(className, Pair.of(parent, serial));
                            serialSet.add(serial);
                        }
                    }

                }
                if(sf.delete()) {
                    sf = new File(path + File.separator + "global.annotation");
                    if(sf.createNewFile()) {
                        globalFile = new RandomAccessFile(sf, "rw");
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                return false;
            }

            roundEnv.getElementsAnnotatedWith(ISerialGenerator.class)
                    .forEach(element->{
                        String pkgName = _ZProcessor.mElementUtils.getPackageOf(element)
                                                                  .getQualifiedName()
                                                                  .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        note(format("annotation class: %s", className));
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
                        final int cs;

                        //update serial-field-count key 必然存在
                        parentCountMap.computeIfPresent(parent, (k, v)->Pair.of(v.fst, v.snd + 1));

                        if(serial > 0) {
                            cs = classMap.computeIfAbsent(className, k->{
                                if(!serialSet.add(serial)) {
                                    String oldName = null;
                                    for(Map.Entry<String, Pair<Integer, Integer>> e : classMap.entrySet()) {
                                        if(e.getValue().snd == serial) {
                                            oldName = e.getKey();
                                            break;
                                        }
                                    }
                                    if(oldName != null) {
                                        for(int i = parent + 1; ; i++) {
                                            if(serialSet.add(i)) {
                                                classMap.put(oldName, Pair.of(parent, i));
                                                break;
                                            }
                                        }
                                    }
                                }
                                return Pair.of(parent, serial);
                            }).snd;
                        }
                        else {
                            cs = classMap.computeIfAbsent(className, k->{
                                for(int i = parent + 1; ; i++) {
                                    if(serialSet.add(i)) {
                                        return Pair.of(parent, i);
                                    }
                                }
                            }).snd;
                        }

                        JCTree tree = (JCTree) _Trees.getTree(element);
                        tree.accept(new SerialTranslator(this, _Context, generator.parent(), cs));
                    });
            try {
                globalFile.setLength(0);
                for(Map.Entry<Integer, Pair<String, Integer>> entry : parentCountMap.entrySet()) {
                    globalFile.write(format("%s:%d=%d\n",
                                            entry.getValue().fst,
                                            entry.getKey(),
                                            entry.getValue().snd).getBytes(StandardCharsets.UTF_8));
                }
                for(Map.Entry<String, Pair<Integer, Integer>> entry : classMap.entrySet()) {
                    globalFile.write(format("%s[%d]@<%d>\n",
                                            entry.getKey(),
                                            entry.getValue().snd,
                                            entry.getValue().fst).getBytes(StandardCharsets.UTF_8));
                }
                globalFile.close();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            try {
                globalFile.close();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void note(String msg)
    {
        _ZProcessor.note(msg);
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
