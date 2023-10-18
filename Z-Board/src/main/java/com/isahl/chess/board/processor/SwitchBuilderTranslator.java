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

import com.isahl.chess.board.processor.model.Child;
import com.sun.source.tree.CaseTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sun.tools.javac.code.TypeTag.INT;

/**
 * @author william.d.zk
 */
public class SwitchBuilderTranslator
        extends TreeTranslator
{
    private final Context               _Context;
    private final java.util.List<Child> _ChildList;
    private final Logger                _Logger = LoggerFactory.getLogger("base.board." + getClass().getSimpleName());

    public SwitchBuilderTranslator(Context context, java.util.List<Child> childList)
    {
        _Context = context;
        _ChildList = childList;
        _Logger.info(childList.toString());
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl clazz)
    {
        super.visitClassDef(clazz);
        final TreeMaker _Maker = TreeMaker.instance(_Context);
        final Names _Names = Names.instance(_Context);
        /*
        ISerial build(int serial){
            return switch(serial){
                case 'child'.serial->new 'child'();
            }
        }
         */

        ListBuffer<JCTree.JCCase> jcCases = new ListBuffer<>();
        for(Child serialClass : _ChildList) {
            String canonicalName = serialClass.name();
            int lp = canonicalName.lastIndexOf(".");
            String pkgName = canonicalName.substring(0, lp);
            String className = canonicalName.substring(lp + 1);
            int serial = serialClass.serial();
            _Logger.debug("case {} → ", serial);

            //@formatter:off
            JCTree.JCCase _case = _Maker.Case(CaseTree.CaseKind.STATEMENT,
                                              List.of(_Maker.Literal(serial)),
                                              List.of(_Maker.Return(_Maker.NewClass(null,
                                                                                    List.nil(),
                                                                                    _Maker.Select(_Maker.Ident(_Names.fromString(pkgName)),
                                                                                                  _Names.fromString(className)),
                                                                                    List.nil(),
                                                                                    null))),
                                              null);
            //@formatter:on
            jcCases.append(_case);
        }

        jcCases.append(_Maker.Case(CaseTree.CaseKind.STATEMENT,
                                   List.of(_Maker.DefaultCaseLabel()),
                                   List.of(_Maker.Return(_Maker.Literal(TypeTag.BOT, null))),
                                   null));

        Name buildParam = _Names.fromString("serial");
        JCTree.JCSwitch jcSwitch = _Maker.Switch(_Maker.Ident(buildParam), jcCases.toList());
        final JCTree.JCBlock _Block = _Maker.Block(0, List.of(jcSwitch));
        JCTree.JCMethodDecl buildMethod = _Maker.MethodDef(_Maker.Modifiers(Flags.PUBLIC),
                                                           _Names.fromString("build"),
                                                           _Maker.Ident(_Names.fromString("ISerial")),
                                                           List.nil(),
                                                           List.of(_Maker.VarDef(_Maker.Modifiers(Flags.PARAMETER),
                                                                                 _Names.fromString("serial"),
                                                                                 _Maker.TypeIdent(INT),
                                                                                 null)),
                                                           List.nil(),
                                                           _Block,
                                                           null);
        clazz.defs = clazz.defs.append(buildMethod);

    }

}
