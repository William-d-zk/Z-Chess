/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2019 Z-Chess                                                
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

package com.tgx.chess.king.base.util.nmea0183;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Idempotent
 * @date 2019/11/29
 */
class GPRMCTest
{
    @Test
    public void test() throws ParseException
    {
        String source = "$GPRMC,081836,A,3751.65,S,14507.36,E,000.0,360.0,130998,011.3,E*62";
        GPRMC gprmc = new GPRMC("$GPRMC,081836,A,3751.65,S,14507.36,E,000.0,360.0,130998,011.3,E*62");
        gprmc.getTimestamp();

        Pattern pattern = Pattern.compile("\\$GPRMC,"
                                          + "([012]\\d[012345]\\d[012345]\\d),"
                                          + "([AV]),"
                                          + "(\\d{4}.\\d{1,4},[NS]),"
                                          + "([01][0-8]\\d{3}.\\d{1,4},[WE]),"
                                          + "(\\d{3}.\\d),"
                                          + "([0123][0-6]\\d.\\d),"
                                          + "([0123]\\d[01]\\d{3}),"
                                          + "(\\d{3}.\\d,[EW]),?"
                                          + "([ADEN]?\\*.+)"
                                          + ".*");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {

            int group_count = matcher.groupCount();
            for (int i = 0; i <= group_count; i++) {
                System.out.println("group:" + i + "  " + matcher.group(i));
            }
        }
    }
}