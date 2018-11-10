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

package com.tgx.chess.spring.biz.bill.pay.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.tgx.chess.spring.jpa.model.AuditModel;

@Entity(name = "Bill")
@Table(schema = "\"tgx-z-chess-bill-pay\"",
       indexes = { @Index(name = "bill_idx_bill", columnList = "bill"),
                   @Index(name = "bill_idx_mac", columnList = "mac"),
                   @Index(name = "bill_idx_open_id", columnList = "openId") })
public class BillEntity
        extends
        AuditModel
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long        id;

    @Column(length = 64, nullable = false, unique = true)
    private String      bill;

    @Column(length = 64)
    private String      openId;

    @Column(length = 17, nullable = false)
    private String      mac;

    private double      amount;

    @Column(length = 6, nullable = false)
    private String      currency = "RMB";

    @Column(length = 16)
    private String      type;

    @Column(length = 20)
    private String      result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemId")
    private ItemEntity item;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getBill()
    {
        return bill;
    }

    public void setBill(String bill)
    {
        this.bill = bill;
    }

    public String getOpenId()
    {
        return openId;
    }

    public void setOpenId(String openId)
    {
        this.openId = openId;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    public double getAmount()
    {
        return amount;
    }

    public void setAmount(double amount)
    {
        this.amount = amount;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    @Override
    public String toString()
    {
        return String.format("bill:%s mac:%s openId:%s amount:%s %s type:%s result:%s", bill, mac, openId, amount, super.toString(), type, result);
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
