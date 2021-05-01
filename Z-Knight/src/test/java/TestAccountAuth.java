
/*
 * MIT License
 * 
 * Copyright (c) 2016~2020. Z-Chess
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.isahl.chess.king.base.util.CryptUtil;

/**
 * @author william.d.zk
 * 
 * @date 2019-07-21
 */
public class TestAccountAuth
{
    public static void main(String[] args)
    {
        CryptUtil _CryptUtil = new CryptUtil();
        System.out.println(_CryptUtil.sha256("AA83B8E5C286510F17C56FF7C588015B" + "smallbeex.mqtt.lbs.tracker"));
        String src = "+/#/#/+";
        src = src.replaceAll("\\++", "+");
        src = src.replaceAll("#+", "#");
        src = src.replaceAll("(/#)+", "/#");

        if (Pattern.compile("#\\+|\\+#")
                   .asPredicate()
                   .test(src))
        { throw new IllegalArgumentException(src); }
        if (!Pattern.compile("(/\\+)$")
                    .asPredicate()
                    .test(src)
            && !Pattern.compile("^\\+")
                       .asPredicate()
                       .test(src))
        {
            src = src.replaceAll("(\\+)$", "");
        }
        src = src.replaceAll("^\\+", "([^\\$/]+)");
        src = src.replaceAll("(/\\+)$", "(/?[^/]*)");
        src = src.replaceAll("/\\+", "/([^/]+)");
        src = src.replaceAll("^#", "([^\\$]*)");
        src = src.replaceAll("^/#", "(/.*)");
        src = src.replaceAll("/#", "(/?.*)");
        System.out.println(src);
        Pattern pattern = Pattern.compile(src);
        System.out.printf("pattern:%s%n", pattern);
        Matcher matcher = pattern.matcher("a/b/c");
        System.out.printf("match:%s%n", matcher.matches() ? matcher.group(): "no matcher");
    }
}
