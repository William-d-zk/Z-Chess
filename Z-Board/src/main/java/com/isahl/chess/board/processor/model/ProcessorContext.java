/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.board.processor.model;

import com.isahl.chess.board.base.ISerial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author william.d.zk
 */
public class ProcessorContext
{

    private final Map<String, SerialClass>  _ClassMap       = new TreeMap<>();
    private final Map<Integer, ParentClass> _ParentCountMap = new TreeMap<>();
    private final Map<Integer, List<Child>> _FactoryMap     = new TreeMap<>();
    private final Set<Integer>              _SerialSet      = new TreeSet<>();

    private final Logger _Logger = LoggerFactory.getLogger("base.board." + getClass().getSimpleName());

    public void init() throws IOException
    {

        onISerialField((parent, field)->_ParentCountMap.put(parent,
                                                            new ParentClass(field, new AtomicInteger(0), parent)));
        RandomAccessFile globalFile = globalFile(true, false);
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
                    _ParentCountMap.put(parent, new ParentClass(field, new AtomicInteger(count), parent));
                }
                matcher = classPattern.matcher(line);
                if(matcher.matches()) {
                    String className = matcher.group(1);
                    int serial = Integer.parseInt(matcher.group(2));
                    int parent = Integer.parseInt(matcher.group(3));
                    _ClassMap.put(className, new SerialClass(parent, serial));
                    _SerialSet.add(serial);
                }
            }
        }

        onISerialField((parent, field)->_FactoryMap.put(parent,
                                                        _ClassMap.entrySet()
                                                                 .stream()
                                                                 .filter(entry->{
                                                                     SerialClass s = entry.getValue();
                                                                     return s.parent() == parent;
                                                                 })
                                                                 .map(entry->{
                                                                     String className = entry.getKey();
                                                                     SerialClass s = entry.getValue();
                                                                     return new Child(className, s.self(), s.parent());
                                                                 })
                                                                 .collect(Collectors.toList())));
    }

    private void reload() throws IOException
    {
        _ClassMap.clear();
        _ParentCountMap.clear();
        _FactoryMap.clear();
        _SerialSet.clear();
        init();
    }

    private void onISerialField(BiConsumer<Integer, String> consumer)
    {
        try {
            for(Field field : ISerial.class.getFields()) {
                int parent = field.getInt(ISerial.class);
                String name = field.getName();
                consumer.accept(parent, name);
            }
        }
        catch(IllegalAccessException e) {
            _Logger.warn("parents handle error", e);
        }
    }

    public List<Child> getChildren(int parent)
    {
        return _FactoryMap.get(parent);
    }

    public void parentIncrementCount(int parent)
    {
        if(_ParentCountMap.containsKey(parent)) {
            _ParentCountMap.get(parent)
                           .count()
                           .getAndIncrement();
        }
        else {
            _Logger.warn(format("parent[ %d ] no define on initializion", parent));
        }
    }

    public int onSerialAnnotation(String className, int annotationSerial, int annotationParent)
    {
        if(annotationSerial > 0) {
            //ISerialGenerator input 手工指定了serial
            return _ClassMap.computeIfAbsent(className, k->{
                                if(!_SerialSet.add(annotationSerial)) {
                                    String oldName = null;
                                    for(Map.Entry<String, SerialClass> e : _ClassMap.entrySet()) {
                                        SerialClass s = e.getValue();
                                        if(s.self() == annotationSerial) {
                                            oldName = e.getKey();
                                            break;
                                        }
                                    }
                                    if(oldName != null) {
                                        SerialClass oldSerial = _ClassMap.get(oldName);
                                        for(int i = oldSerial.parent() + 1; ; i++) {
                                            if(_SerialSet.add(i)) {
                                                _ClassMap.put(oldName, new SerialClass(oldSerial.parent(), i));
                                                break;
                                            }
                                        }
                                    }
                                }
                                return new SerialClass(annotationParent, annotationSerial);
                            })
                            .self();

        }
        else {
            return _ClassMap.computeIfAbsent(className, k->{
                                for(int i = annotationParent + 1; ; i++) {
                                    if(_SerialSet.add(i)) {
                                        return new SerialClass(annotationParent, i);
                                    }
                                }
                            })
                            .self();
        }
    }

    public void update() throws IOException
    {
        RandomAccessFile globalFile = globalFile(false, true);
        for(ParentClass parent : _ParentCountMap.values()) {
            globalFile.write(format("%s:%d=%d\n",
                                    parent.name(),
                                    parent.self(),
                                    parent.count()
                                          .get()).getBytes(StandardCharsets.UTF_8));
        }
        for(Map.Entry<String, SerialClass> entry : _ClassMap.entrySet()) {
            String className = entry.getKey();
            SerialClass s = entry.getValue();
            globalFile.write(format("%s[%d]@<%d>\n", className, s.self(), s.parent()).getBytes(StandardCharsets.UTF_8));
        }
        globalFile.close();
        reload();
    }

    private RandomAccessFile globalFile(boolean readonly, boolean delete) throws IOException
    {
        try {
            return new RandomAccessFile(getGlobalFile(delete), readonly ? "r" : "rw");
        }
        catch(IOException e) {
            _Logger.warn("global file failed", e);
            throw e;
        }
    }

    private File getGlobalFile(boolean delete) throws IOException
    {
        String path = System.getProperty("user.home") + File.separator + "Z-Chess" + File.separator + "processor";
        File directory = new File(path);
        File sf;
        if(!directory.exists() && !directory.mkdirs()) {
            throw new IOException("directory create failed");
        }
        sf = new File(path + File.separator + "global-annotation.log");
        if(!delete && !sf.exists() && !sf.createNewFile()) {
            throw new IOException("file create failed");
        }
        else if(sf.exists() && delete && sf.delete() && sf.createNewFile()) {
            return sf;
        }
        else if(sf.exists()) {
            return sf;
        }
        throw new IOException("file no exist");
    }

}
