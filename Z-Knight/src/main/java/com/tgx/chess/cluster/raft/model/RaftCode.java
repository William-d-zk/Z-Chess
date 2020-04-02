/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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

package com.tgx.chess.cluster.raft.model;

public enum RaftCode
{
    SUCCESS(0, "success"),
    LOWER_TERM(1, "term < current,reject"),
    INCORRECT_TERM(2, "pre-log-index&pre-log-term inconsistent,reject"),
    ILLEGAL_STATE(3, "illegal state,reject"),
    SPLIT_CLUSTER(4, "split cluster,reject"),
    ALREADY_VOTE(5, "already vote,reject"),
    OBSOLETE(6, "index obsolete,reject");

    private final int    _Code;
    private final String _Description;

    RaftCode(int code,
             String des)
    {
        _Code = code;
        _Description = des;
    }

    public int getCode()
    {
        return _Code;
    }

    public String getDescription()
    {
        return _Description;
    }

    static RaftCode valueOf(int code)
    {
        switch (code)
        {
            case 0:
                return SUCCESS;
            case 1:
                return LOWER_TERM;
            case 2:
                return INCORRECT_TERM;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
