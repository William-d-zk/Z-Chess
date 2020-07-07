/*
 * MIT License                                                                   
 *                                                                               
 * Copyright (c) 2016~2020. Z-Chess                                              
 *                                                                               
 * Permission is hereby granted, free of charge, to any person obtaining a copy  
 * of this software and associated documentation files (the "Software"), to deal 
 * in the Software without restriction, including without limitation the rights  
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     
 * copies of the Software, and to permit persons to whom the Software is         
 * furnished to do so, subject to the following conditions:                      
 *                                                                               
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.                               
 *                                                                               
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.                                                                      
 */

package com.tgx.chess.king.base.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.king.base.inf.ICode;
import com.tgx.chess.king.config.Code;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ZResponse<T>
        implements
        ICode
{
    private final int    _Code;
    private final String _Message;
    private final T      _Detail;
    private final String _Formatter;

    @JsonCreator
    public ZResponse(int code,
                     String message,
                     T detail,
                     String formatter)
    {
        _Code = code;
        _Message = message;
        _Detail = detail;
        _Formatter = formatter;
    }

    public int getCode()
    {
        return _Code;
    }

    @Override
    public String format(Object... args)
    {
        return _Formatter == null ? null
                                  : String.format(_Formatter, args);
    }

    public String getMessage()
    {
        return _Message;
    }

    public T getDetail()
    {
        return _Detail;
    }

    public static <E> ZResponse<E> success(E e)
    {
        return new ZResponse<>(Code.SUCCESS.getCode(), Code.SUCCESS.format(), e, Code.SUCCESS.getFormatter());
    }

    public static <E> ZResponse<E> forbid(E e)
    {
        return new ZResponse<>(Code.FORBIDDEN.getCode(), Code.FORBIDDEN.format(e), e, Code.FORBIDDEN.getFormatter());
    }

    public static <E> ZResponse<E> unauthorized(E e)
    {
        return new ZResponse<>(Code.UNAUTHORIZED.getCode(),
                               Code.UNAUTHORIZED.format(e),
                               e,
                               Code.UNAUTHORIZED.getFormatter());
    }
}
