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

package com.tgx.chess.spring.biz.bill.pay.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.ZApiExecption;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.spring.biz.bill.pay.Result;
import com.tgx.chess.spring.biz.bill.pay.Type;
import com.tgx.chess.spring.biz.bill.pay.api.dao.BillEntry;
import com.tgx.chess.spring.biz.bill.pay.model.BillEntity;
import com.tgx.chess.spring.biz.bill.pay.service.BillService;

@RestController
public class BillController
{
    private final Logger      _Log = Logger.getLogger(getClass().getName());

    private final BillService _BillService;

    @Autowired
    public BillController(BillService billService)
    {
        _BillService = billService;
    }

    @GetMapping("/bill/pay")
    public @ResponseBody BillEntry pay(@RequestParam("type") Type type,
                                       @RequestParam("bill") String bill,
                                       @RequestParam("mac") String mac,
                                       @RequestParam("oid") String openId,
                                       @RequestParam("amount") double amount) throws ZApiExecption
    {
        BillEntity billEntity = new BillEntity();
        billEntity.setAmount(amount);
        billEntity.setBill(bill);
        billEntity.setMac(mac);
        billEntity.setOpenId(openId);
        billEntity.setResult(Result.PENDING.name());
        billEntity.setType(type.name());
        _BillService.saveBill(billEntity);
        BillEntry billEntry = new BillEntry();
        billEntry.setBill(bill);
        billEntry.setMac(mac);
        return billEntry;
    }
}
