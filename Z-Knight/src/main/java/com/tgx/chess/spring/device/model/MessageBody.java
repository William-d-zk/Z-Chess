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

package com.tgx.chess.spring.device.model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * @date 2019-07-31
 */

public class MessageBody
        implements
        Serializable
{
    private static Logger       _Logger          = Logger.getLogger(MessageBody.class.getSimpleName());
    private static final long   serialVersionUID = -8904730289818144372L;
    private static ObjectMapper jsonMapper       = new ObjectMapper();

    private String topic;
    private byte[] content;

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    @JsonIgnore
    public byte[] getPayload()
    {
        return content;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public JsonNode getContent()
    {
        if (content == null || content.length == 0) {
            _Logger.warning("content null");
            return null;
        }
        try {
            return jsonMapper.readTree(content);
        }
        catch (Exception e) {
            try {
                RawContent content = new RawContent();
                content.setPayload(this.content);
                try {
                    content.setRaw(new String(this.content, StandardCharsets.UTF_8));
                }
                catch (Exception stre) {
                    _Logger.debug(String.format("content:%s", IoUtil.bin2Hex(this.content, ":")));
                    //ignore
                }
                return jsonMapper.valueToTree(content);
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public void setContent(JsonNode content)
    {
        setPayload(content.toString()
                          .getBytes(StandardCharsets.UTF_8));
    }

    public void setPayload(byte[] content)
    {
        this.content = content;
    }

}
