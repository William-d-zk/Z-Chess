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
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SupportedAnnotationTypes("com.isahl.chess.board.annotation.ISerialGenerator")
public class SerialProcessor
        extends AbstractProcessor
{

    private Filer                      mFiler;
    private Messager                   mMessager;
    private Elements                   mElementUtils;
    private JavacProcessingEnvironment mEnvironment;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        mEnvironment = (JavacProcessingEnvironment) processingEnv;
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
        note("custom processor: %s", getClass().getSimpleName());
    }

    private void note(String msg)
    {
        mMessager.printMessage(Diagnostic.Kind.NOTE, msg);
        System.out.println(msg);
    }

    private void note(String format, Object... args)
    {
        mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args));
        System.out.println(String.format(format, args));
    }

    private void write(Writer writer, StringBuilder builder, String... contents) throws IOException
    {
        for(String str : contents) {
            writer.write(str);
            builder.append(str);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if(!roundEnv.processingOver()) {
            final Context _Context = mEnvironment.getContext();
            final Trees _Trees = Trees.instance(mEnvironment);

            Map<Integer, Integer> globalMap = new HashMap<>();
            Map<String, Integer> classMap = new HashMap<>();
            Map<String, Pair<Integer, Integer>> fieldMap = new HashMap<>();
            try {
                for(Field field : ISerial.class.getFields()) {
                    int value = field.getInt(ISerial.class);
                    String name = field.getName();
                    fieldMap.put(name, Pair.of(value, 0));
                    globalMap.put(value, 0);
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
                    Pattern classPattern = Pattern.compile("(.+)\\[(\\d+)]");
                    while((line = globalFile.readLine()) != null) {
                        Matcher matcher = fieldPattern.matcher(line);
                        if(matcher.matches()) {
                            String field = matcher.group(1);
                            int key = Integer.parseInt(matcher.group(2));
                            int count = Integer.parseInt(matcher.group(3));
                            globalMap.put(key, count);
                            fieldMap.put(field, Pair.of(key, count));
                        }
                        matcher = classPattern.matcher(line);
                        if(matcher.matches()) {
                            String className = matcher.group(1);
                            int serial = Integer.parseInt(matcher.group(2));
                            classMap.put(className, serial);
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
                        String pkgName = mElementUtils.getPackageOf(element)
                                                      .getQualifiedName()
                                                      .toString();
                        String className = pkgName + "." + element.getSimpleName()
                                                                  .toString();
                        note("annotation class:%s", className);
                        boolean hasSerial = false;
                        boolean hasSuper = false;
                        List<? extends Element> enclosedElements = element.getEnclosedElements();
                        for(Element enclosed : enclosedElements) {
                            if(enclosed.getKind() == ElementKind.METHOD) {
                                switch(enclosed.getSimpleName()
                                               .toString()) {
                                    case "serial" -> hasSerial = true;
                                    case "_super" -> hasSuper = true;
                                }
                            }
                        }
                        ISerialGenerator generator = element.getAnnotation(ISerialGenerator.class);
                        int parent = generator.parent();
                        int serial = generator.serial();
                        final int cs;
                        if(serial < 0 && !classMap.containsKey(className)) {
                            serial = parent + globalMap.computeIfPresent(parent, (k, v)->v + 1);
                            note("+ class:%s,%d [%d]", className, parent, serial);
                        }
                        if(classMap.containsKey(className)) {
                            serial = classMap.get(className);
                            note("= class:%s,%d [%d]", className, parent, serial);
                        }
                        if(serial > 0) {
                            int count = serial - parent;
                            globalMap.computeIfPresent(parent, (k, v)->Math.max(count, v));
                            note("- class:%s,%d [%d]", className, parent, serial);
                        }
                        cs = serial;

                        JCTree tree = (JCTree) _Trees.getTree(element);
                        tree.accept(new SerialTranslator(_Context,
                                                         hasSuper,
                                                         hasSerial,
                                                         generator.parent(),
                                                         classMap.computeIfAbsent(className, (ck)->cs)));
                    });
            try {
                globalFile.setLength(0);
                for(Map.Entry<String, Pair<Integer, Integer>> entry : fieldMap.entrySet()) {
                    globalFile.write(String.format("%s:%d=%d\r\n",
                                                   entry.getKey(),
                                                   entry.getValue().fst,
                                                   globalMap.get(entry.getValue().fst))
                                           .getBytes(StandardCharsets.UTF_8));
                }
                for(Map.Entry<String, Integer> entry : classMap.entrySet()) {
                    globalFile.write(String.format("%s[%d]\r\n", entry.getKey(), entry.getValue())
                                           .getBytes(StandardCharsets.UTF_8));
                }

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

}
