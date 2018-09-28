/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.spring.jpa.model.AuditModel;

@Entity
@Table(schema = "\"tgx-z-chess-device\"")
public class Device
        extends
        AuditModel
{
    private static final long serialVersionUID = -6645586986057373344L;

    @Id
    @GeneratedValue(generator = "ZGenerator")
    @GenericGenerator(name = "ZGenerator", strategy = "com.tgx.chess.spring.jpa.generator.ZGenerator")
    private Long              id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    //    @Length(min = 32, max = 32, message = "device serial number [SHA256]")
    @NotEmpty(message = "provide create device sn")
    @Basic(fetch = FetchType.LAZY)
    private byte[] sn;

    @Length(min = 5, message = "*Your password must have at least 5 characters")
    @NotEmpty(message = "*Please provide your password")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] getSn() {
        return sn;
    }

    public void setSn(byte[] sn) {
        this.sn = sn;
    }

    @Override
    public String toString() {
        return String.format("device{id:%s,pass:%s,sn:%s,create:%s,update:%s}",
                             getId(),
                             getPassword(),
                             IoUtil.bin2Hex(getSn()),
                             getCreatedAt(),
                             getUpdatedAt());
    }
}
