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

package com.isahl.chess.square.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultTaskExecutor
        implements TaskExecutor
{
    private static final Logger _Logger = LoggerFactory.getLogger(DefaultTaskExecutor.class);
    private static final ObjectMapper _Mapper = new ObjectMapper();
    private final Map<String, TaskHandler> _Handlers = new ConcurrentHashMap<>();

    @Autowired
    public DefaultTaskExecutor()
    {
        registerDefaultHandlers();
    }

    private void registerDefaultHandlers()
    {
        _Handlers.put("ping", payload -> "pong");
        _Handlers.put("echo", payload -> payload);
        _Handlers.put("status", payload -> "OK");
    }

    public void registerHandler(String type, TaskHandler handler)
    {
        _Handlers.put(type, handler);
    }

    @Override
    public String execute(String payload) throws Exception
    {
        if(payload == null || payload.isEmpty()) {
            return "{\"status\":\"ok\"}";
        }
        try {
            JsonNode node = _Mapper.readTree(payload);
            String type = node.has("type") ? node.get("type").asText() : "unknown";
            TaskHandler handler = _Handlers.get(type);
            if(handler != null) {
                String result = handler.handle(payload);
                _Logger.debug("Executed task type={}", type);
                return result;
            }
            else {
                _Logger.warn("No handler for task type={}", type);
                return "{\"error\":\"unknown task type: " + type + "\"}";
            }
        }
        catch(Exception e) {
            _Logger.error("Failed to execute task", e);
            throw e;
        }
    }

    @FunctionalInterface
    public interface TaskHandler
    {
        String handle(String payload) throws Exception;
    }
}