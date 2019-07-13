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

package com.tgx.chess.spring.biz.bill.pay.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tgx.chess.spring.biz.bill.pay.model.BillEntity;
import com.tgx.chess.spring.biz.bill.pay.model.ItemEntity;

/**
 * @author william.d.zk
 */
@Repository
public interface BillRepository
        extends
        JpaRepository<BillEntity,
                      Long>
{

    List<BillEntity> findAllByMac(String mac);

    List<BillEntity> findAllByOpenId(String openId);

    BillEntity findByBill(String bill);

    BillEntity findFirstByMacAndItem(String mac, ItemEntity item, Sort sort);

}
