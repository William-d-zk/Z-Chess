/*
 * MIT License
 *
 * Copyright (c) 2022~2022. Z-Chess
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

package com.isahl.chess.board.processor.services;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.helpers.Util;

import java.net.URL;

public class BoardLogLoader
        extends ContextAwareBase
        implements Configurator
{
    public void configure(LoggerContext loggerContext)
    {
        this.addInfo("Setting up retail default configuration.");
        // 清除loggerContext已加载配置，重新加载
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        try {
            //  获取jar中默认配置文件路径
            URL url = Configurator.class.getClassLoader()
                                        .getResource("logback-board.xml");
            // 设置loggerContext到JoranConfigurator
            configurator.setContext(loggerContext);
            // 加载默认配置
            configurator.doConfigure(url);
        }
        catch(JoranException e) {
            Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", e);
        }
    }
}