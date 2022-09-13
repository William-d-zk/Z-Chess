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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

/**
 * @author william.d.zk
 */
public class FactoryTranslator
        extends TreeTranslator
{
    private final Context          _Context;
    private final int              _Serial;
    private final FactoryProcessor _Processor;

    public FactoryTranslator(FactoryProcessor processor, Context context, int serial)
    {
        _Processor = processor;
        _Context = context;
        _Serial = serial;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl clazz)
    {
        super.visitClassDef(clazz);
        final TreeMaker _Maker = TreeMaker.instance(_Context);
        final Names _Names = Names.instance(_Context);
        final Symtab _Symtab = Symtab.instance(_Context);
        if(!_Processor.existMethodSerial()) {
            final JCTree.JCExpression _SerialValue = _Maker.Literal(_Serial);
            final JCTree.JCBlock _Block = _Maker.Block(0, List.of(_Maker.Return(_SerialValue)));
            clazz.defs = clazz.defs.append(_Maker.MethodDef(_Maker.Modifiers(Flags.PUBLIC),
                                                            _Names.fromString("serial"),
                                                            _Maker.Type(_Symtab.intType),
                                                            List.nil(),
                                                            List.nil(),
                                                            List.nil(),
                                                            _Block,
                                                            null));
        }

        /*
        if(_Processor.existFieldSerial()) {
            for(JCTree tree : clazz.defs) {
                if(tree.getKind() == Tree.Kind.VARIABLE && tree.toString()
                                                               .contains("_SERIAL"))
                {
                    JCTree.JCVariableDecl x = (JCTree.JCVariableDecl) tree;
                    x.init = _Maker.Literal(_Serial);
                }
            }
        }
        else {
            final JCTree.JCExpression _SerialValue = _Maker.Literal(_Serial);
            clazz.defs = clazz.defs.append(_Maker.VarDef(_Maker.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.FINAL),
                                                         _Names.fromString("_SERIAL"),
                                                         _Maker.Type(_Symtab.intType),
                                                         _SerialValue));
        }

         */
    }
}
