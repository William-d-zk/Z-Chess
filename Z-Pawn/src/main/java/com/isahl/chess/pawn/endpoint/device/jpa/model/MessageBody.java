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

package com.isahl.chess.pawn.endpoint.device.jpa.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.pawn.endpoint.device.model.RawContent;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * @author william.d.zk
 * @date 2019-07-31
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageBody
        implements Serializable
{
    private static final Logger _Logger          = Logger.getLogger(
            "endpoint.pawn." + MessageBody.class.getSimpleName());
    @Serial
    private static final long   serialVersionUID = -8904730289818144372L;

    private final int    _MsgId;
    private final String _Topic;
    private final byte[] _Content;

    @JsonCreator
    public MessageBody(
            @JsonProperty("msg_id")
                    int msgId,
            @JsonProperty("topic")
                    String topic,
            @JsonProperty("content")
                    JsonNode content)
    {
        _MsgId = msgId;
        _Topic = topic;
        _Content = content.toString()
                          .getBytes(StandardCharsets.UTF_8);
    }

    public int getMsgId()
    {
        return _MsgId;
    }

    public String getTopic()
    {
        return _Topic;
    }

    public JsonNode getContent()
    {
        if(_Content == null || _Content.length == 0) {
            _Logger.warning("content null");
            return null;
        }
        try {
            return JsonUtil.readTree(_Content);
        }
        catch(Exception e) {
            try {
                RawContent content = new RawContent();
                content.setPayload(_Content);
                try {
                    content.setRaw(new String(_Content, StandardCharsets.UTF_8));
                }
                catch(Exception stre) {
                    _Logger.debug(String.format("content:%s", IoUtil.bin2Hex(_Content, ":")));
                    // ignore
                }
                return JsonUtil.valueToTree(content);
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public byte[] contentBinary()
    {
        if(_Content == null || _Content.length == 0) {
            _Logger.warning("content null");
            return null;
        }
        return _Content;
    }

}
