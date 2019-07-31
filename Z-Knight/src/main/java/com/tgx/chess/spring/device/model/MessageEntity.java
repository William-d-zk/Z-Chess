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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.tgx.chess.spring.jpa.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

/**
 * @author william.d.zk
 * @date 2019-07-22
 */
@Entity(name = "Message")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(schema = "\"tgx-z-chess-device\"")
public class MessageEntity
        extends
        AuditModel
        implements
        Serializable
{

    private static final long serialVersionUID = -6502547239976531057L;

    @Id
    @GeneratedValue(generator = "ZGenerator")
    @GenericGenerator(name = "ZGenerator", strategy = "com.tgx.chess.spring.jpa.generator.ZGenerator")
    private Long    id;
    @Column(updatable = false)
    private long    origin;
    @Column(updatable = false)
    private int     cmd;
    @Column(length = 4, nullable = false, updatable = false)
    private String  direction;
    @Column(length = 10, nullable = false)
    private String  owner = "SERVER";
    @Column(updatable = false)
    private int     localId;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private JsonMsg payload;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public JsonMsg getPayload()
    {
        return payload;
    }

    public void setPayload(JsonMsg payload)
    {
        this.payload = payload;
    }

    public String getDirection()
    {
        return direction;
    }

    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public int getLocalId()
    {
        return localId;
    }

    public void setLocalId(int localId)
    {
        this.localId = localId;
    }

    public int getCmd()
    {
        return cmd;
    }

    public void setCmd(int cmd)
    {
        this.cmd = cmd;
    }

    public long getOrigin()
    {
        return origin;
    }

    public void setOrigin(long origin)
    {
        this.origin = origin;
    }
}
