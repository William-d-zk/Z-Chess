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

package com.tgx.chess.spring.biz.bill.pay.api;

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
import com.tgx.chess.spring.biz.bill.pay.api.dao.ItemEntry;
import com.tgx.chess.spring.biz.bill.pay.model.ItemEntity;
import com.tgx.chess.spring.biz.bill.pay.service.ItemsService;

@RestController
public class ItemsController
{
    private final Logger       _Log = Logger.getLogger(getClass().getName());

    private final ItemsService _ItemsService;

    @Autowired
    public ItemsController(ItemsService itemsService)
    {
        _ItemsService = itemsService;
    }

    @GetMapping("/items/add")
    public @ResponseBody ItemEntry addSku(@RequestParam(name = "sku") String sku,
                                          @RequestParam(name = "price") double price,
                                          @RequestParam(name = "currency", defaultValue = "CNY") String currency) throws ZApiExecption
    {
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setPrice(price);
        itemEntity.setSku(sku);
        itemEntity = _ItemsService.addItem(itemEntity);
        ItemEntry itemEntry = new ItemEntry();
        itemEntry.setId(itemEntity.getId());
        itemEntry.setCurrency(currency);
        itemEntry.setPrice(price);
        itemEntry.setSku(sku);
        return itemEntry;
    }

    @GetMapping("/items/list")
    public @ResponseBody List<ItemEntry> listSku() throws ZApiExecption
    {
        return _ItemsService.listItems()
                            .stream()
                            .filter(Objects::nonNull)
                            .map(itemEntity ->
                            {
                                ItemEntry itemEntry = new ItemEntry();
                                itemEntry.setSku(itemEntity.getSku());
                                itemEntry.setPrice(itemEntity.getPrice());
                                itemEntry.setCurrency(itemEntity.getCurrency());
                                return itemEntry;
                            })
                            .collect(Collectors.toList());

    }
}
