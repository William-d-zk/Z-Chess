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

package api;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.ZApiExecption;
import com.tgx.chess.king.base.log.Logger;

import dao.BillEntry;
import model.BillEntity;
import model.ItemEntity;
import pay.Result;
import service.BillService;
import service.ItemsService;

/**
 * @author william.d.zk
 */
@RestController
public class BillController
{
    private final Logger _Log = Logger.getLogger(getClass().getName());

    private final BillService  _BillService;
    private final ItemsService _ItemsService;

    @Autowired
    public BillController(BillService billService,
                          ItemsService itemsService)
    {
        _BillService = billService;
        _ItemsService = itemsService;
    }

    @GetMapping("/bill/pay")
    public @ResponseBody BillEntry pay(@RequestParam("type") String type,
                                       @RequestParam("bill") String bill,
                                       @RequestParam("mac") String mac,
                                       @RequestParam("amount") double amount,
                                       @RequestParam("item") long id) throws ZApiExecption
    {
        BillEntity billEntity = new BillEntity();
        billEntity.setAmount(amount);
        billEntity.setBill(bill);
        billEntity.setMac(mac);
        billEntity.setResult(Result.PENDING.name());
        billEntity.setType(type);
        billEntity.setItem(_ItemsService.findById(id)
                                        .orElseThrow(() -> new ZApiExecption(String.format("undefine item %d", id))));
        try {
            _BillService.saveBill(billEntity);
        }
        catch (Exception pse) {
            pse.printStackTrace();
            throw new ZApiExecption(String.format("duplicate key - bill %s", bill));

        }
        _Log.info(billEntity.toString());

        BillEntry billEntry = new BillEntry();
        billEntry.setBill(bill);
        billEntry.setMac(mac);
        billEntry.setStatus(Result.PENDING.name());
        billEntry.setTimestamp(billEntity.getUpdatedAt());
        billEntry.setItem(billEntity.getItem()
                                    .getId());
        return billEntry;
    }

    @GetMapping("/bill/confirm")
    public @ResponseBody BillEntry confirm(@RequestParam("bill") String bill,
                                           @RequestParam("oid") String openId,
                                           @RequestParam("result") boolean result) throws ZApiExecption
    {
        BillEntity billEntity = _BillService.findByBill(bill)
                                            .orElseThrow(() -> new ZApiExecption(String.format("bill %s not exist!",
                                                                                               bill)));
        billEntity.setResult(result ? Result.SUCCESS.name()
                                    : Result.FAILED.name());
        billEntity.setOpenId(openId);
        billEntity = _BillService.saveBill(billEntity);
        BillEntry billEntry = new BillEntry();
        billEntry.setItem(billEntity.getItem()
                                    .getId());
        billEntry.setBill(bill);
        billEntry.setMac(billEntity.getMac());
        billEntry.setStatus(billEntity.getResult());
        billEntry.setTimestamp(billEntity.getUpdatedAt());
        return billEntry;
    }

    @GetMapping("/bill/list")
    public @ResponseBody List<BillEntry> query(@RequestParam("mac") String mac) throws ZApiExecption
    {
        return _BillService.findAllByMac(mac)
                           .stream()
                           .filter(Objects::nonNull)
                           .map(billEntity ->
                           {
                               BillEntry billEntry = new BillEntry();
                               billEntry.setStatus(billEntity.getResult());
                               billEntry.setMac(mac);
                               billEntry.setBill(billEntity.getBill());
                               billEntry.setItem(billEntity.getItem()
                                                           .getId());
                               billEntry.setTimestamp(billEntity.getUpdatedAt());
                               return billEntry;
                           })
                           .collect(Collectors.toList());
    }

    @GetMapping("/bill/query")
    public @ResponseBody BillEntry queryLastBillByItem(@RequestParam("mac") String mac, @RequestParam("item") long item)
    {
        ItemEntity itemEntity = _ItemsService.findById(item)
                                             .orElseThrow(() -> new ZApiExecption(String.format("item not define %d",
                                                                                                item)));
        BillEntity billEntity = _BillService.findLastByMacAndItem(mac, itemEntity);
        if (Objects.isNull(billEntity)) {
            throw new ZApiExecption(String.format("not found bill entity by mac:%s ", mac));
        }
        BillEntry billEntry = new BillEntry();
        billEntry.setMac(billEntity.getMac());
        billEntry.setBill(billEntity.getBill());
        billEntry.setStatus(billEntity.getResult());
        billEntry.setItem(billEntity.getItem()
                                    .getId());
        billEntry.setTimestamp(billEntity.getUpdatedAt());
        return billEntry;
    }

}
