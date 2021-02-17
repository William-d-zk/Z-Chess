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

package com.isahl.chess.king.base.util;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.isahl.chess.king.base.log.Logger;

public class JsonUtil
{
    private final static Logger       _Logger       = Logger.getLogger(JsonUtil.class.getSimpleName());
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        String STANDARD_PATTERN = "yyyy-MM-dd HH:mm:ss";
        String DATE_PATTERN = "yyyy-MM-dd";
        String TIME_PATTERN = "HH:mm:ss";
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(STANDARD_PATTERN);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        //处理LocalDate
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        //处理LocalTime
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        OBJECT_MAPPER.registerModules(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES),
                                      new Jdk8Module(),
                                      javaTimeModule);
        OBJECT_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        OBJECT_MAPPER.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        OBJECT_MAPPER.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    }

    public static <T> T readValue(byte[] input, Class<T> clazz)
    {
        if (input == null) return null;
        try {
            return OBJECT_MAPPER.readValue(input, clazz);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json[%s] error with %s", IoUtil.bin2Hex(input, ":"), clazz, e);
        }
        return null;
    }

    public static <T> T readValue(String input, Class<T> clazz)
    {
        if (IoUtil.isBlank(input)) return null;
        try {
            return OBJECT_MAPPER.readValue(input, clazz);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json[%s] error with %s", input, clazz, e);
        }
        return null;
    }

    public static <T> T readValue(byte[] input, TypeReference<T> type)
    {
        if (input == null) return null;
        try {
            return OBJECT_MAPPER.readValue(input, type);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json[%s] error with %s", IoUtil.bin2Hex(input, ":"), type, e);
        }
        return null;
    }

    public static <T> T readValue(String input, TypeReference<T> type)
    {
        if (IoUtil.isBlank(input)) return null;
        try {
            return OBJECT_MAPPER.readValue(input, type);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json[%s] error with %s", input, type, e);
        }
        return null;
    }

    public static <T> T readValue(InputStream input, TypeReference<T> type)
    {
        if (input == null) return null;
        try {
            return OBJECT_MAPPER.readValue(input, type);
        }
        catch (JsonParseException |
               JsonMappingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
            _Logger.warning("read json error with %s", type, e);
        }
        return null;
    }

    public static JsonNode readTree(byte[] input)
    {
        if (input == null) return OBJECT_MAPPER.nullNode();
        try {
            return OBJECT_MAPPER.readTree(input);
        }
        catch (IOException e) {
            _Logger.warning("read tree input[%s] error", IoUtil.bin2Hex(input, ":"), e);
            return OBJECT_MAPPER.nullNode();
        }
    }

    public static JsonNode readTree(String input)
    {
        if (IoUtil.isBlank(input)) return OBJECT_MAPPER.nullNode();
        try {
            return OBJECT_MAPPER.readTree(input);
        }
        catch (IOException e) {
            _Logger.warning("read tree input[%s] error", input, e);
            return OBJECT_MAPPER.nullNode();
        }
    }

    public static JsonNode readTree(InputStream input)
    {
        if (input == null) return OBJECT_MAPPER.nullNode();
        try {
            return OBJECT_MAPPER.readTree(input);
        }
        catch (IOException e) {
            _Logger.warning("read tree input stream error", e);
            return OBJECT_MAPPER.nullNode();
        }
    }

    public static <T> JsonNode valueToTree(T data)
    {
        return OBJECT_MAPPER.valueToTree(data);
    }

    public static <T> byte[] writeValueAsBytes(T input)
    {
        if (input == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsBytes(input);
        }
        catch (JsonProcessingException e) {
            _Logger.warning("write json error", e);
        }
        return null;
    }

    public static <T> String writeValueAsString(T input)
    {
        if (input == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(input);
        }
        catch (JsonProcessingException e) {
            _Logger.warning("write json error", e);
        }
        return null;
    }

    public static byte[] writeNodeAsBytes(JsonNode input)
    {
        if (input == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsBytes(input);
        }
        catch (JsonProcessingException e) {
            _Logger.warning("write json error", e);
        }
        return null;

    }

}
