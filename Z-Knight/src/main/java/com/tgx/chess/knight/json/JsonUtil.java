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

package com.tgx.chess.knight.json;

import java.io.IOException;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.tgx.chess.king.base.log.Logger;

@ConfigurationProperties(prefix = "spring.jackson")
@Configuration
public class JsonUtil
{
    private final static Logger _Logger      = Logger.getLogger(JsonUtil.class.getSimpleName());
    private static ObjectMapper objectMapper = new ObjectMapper();

    public void setPropertyNamingStrategy(String input)
    {
        PropertyNamingStrategy strategy;
        switch (input)
        {
            case "KEBAB_CASE":
                strategy = PropertyNamingStrategy.KEBAB_CASE;
                break;
            case "LOWER_CAMEL_CASE":
                strategy = PropertyNamingStrategy.LOWER_CAMEL_CASE;
                break;
            case "LOWER_CASE":
                strategy = PropertyNamingStrategy.LOWER_CASE;
                break;
            case "LOWER_DOT_CASE":
                strategy = PropertyNamingStrategy.LOWER_DOT_CASE;
                break;
            case "SNAKE_CASE":
                strategy = PropertyNamingStrategy.SNAKE_CASE;
                break;
            case "UPPER_CAMEL_CASE":
                strategy = PropertyNamingStrategy.UPPER_CAMEL_CASE;
                break;
            default:
                return;
        }
        objectMapper.setPropertyNamingStrategy(strategy);
    }

    public static <T> T readValue(byte[] input, Class<T> clazz)
    {
        try {
            return objectMapper.readValue(input, clazz);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json error", e);
        }
        return null;
    }

    public static <T> T readValue(byte[] input, TypeReference<T> type)
    {
        try {
            return objectMapper.readValue(input, type);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json error", e);
        }
        return null;
    }

    public static JsonNode readTree(byte[] data) throws IOException
    {
        return objectMapper.readTree(data);
    }

    public static <T> JsonNode valueToTree(T data)
    {
        return objectMapper.valueToTree(data);
    }

    public static <T> byte[] writeValueAsBytes(T input)
    {
        try {
            return objectMapper.writeValueAsBytes(input);
        }
        catch (JsonProcessingException e) {
            _Logger.warning("write json error", e);
        }
        return null;
    }

    public static <T> String writeValueAsString(T input)
    {
        try {
            return objectMapper.writeValueAsString(input);
        }
        catch (JsonProcessingException e) {
            _Logger.warning("write json error", e);
        }
        return null;
    }
}
