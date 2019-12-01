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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tgx.chess.king.base.log.Logger;

/**
 * @author Idempotent
 * @date 2019/11/28
 */
public class GPRMC
{
    private final static Logger _Logger = Logger.getLogger(GPRMC.class.getSimpleName());
    private final Matcher       _Matcher;

    private final boolean _IsHolder;

    public GPRMC(String raw)
    {
        _Matcher = protocol.matcher(raw);
        _IsHolder = true;
    }

    public GPRMC()
    {
        _Matcher = null;
        _IsHolder = false;
    }

    private String timestamp;

    private static Pattern   protocol            = Pattern.compile("\\$GPRMC,"
                                                                   + "([012]\\d[012345]\\d[012345]\\d),"
                                                                   + "([AV]),"
                                                                   + "([0-8]\\d{3}.\\d{1,4},[NS]),"
                                                                   + "([01]\\d{4}.\\d{1,4},[WE]),"
                                                                   + "(\\d{3}.\\d),"
                                                                   + "([0123][0-5]\\d.\\d{2}),"
                                                                   + "([0123]\\d[01]\\d{3}),"
                                                                   + "(\\d{3}.\\d,[EW]),?"
                                                                   + "([ADEN]?\\*.+)");

    private static final int GROUP_1_UTC         = 1;
    private static final int GROUP_2_STATUS      = 2;
    private static final int GROUP_3_LATITUDE    = 3;
    private static final int GROUP_4_LONGITUDE   = 4;
    private static final int GROUP_5_SPEED       = 5;
    private static final int GROUP_6_DIRECTION   = 6;
    private static final int GROUP_7_DATE        = 7;
    private static final int GROUP_8_DECLINATION = 8;
    private static final int GROUP_9_MODEL       = 9;

    private boolean checkProtocol(String raw)
    {
        Matcher matcher = protocol.matcher(raw);
        _Logger.info(matcher.group());
        return matcher.matches();
    }

    public String getTimestamp() throws ParseException
    {

        if (_Matcher.matches()) {
            String date = _Matcher.group(7);
            String time = _Matcher.group(1);
            SimpleDateFormat format = new SimpleDateFormat("ddmmyy hhmmss");
            Date data = format.parse(date + " " + time);
            _Logger.info(data);
        }
        else {
            for (; _Matcher.find();) {

            }
        }
        return "UNKNOWN";
    }
}
