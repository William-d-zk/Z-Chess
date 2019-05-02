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

package com.tgx.chess.spring.biz.bill.pay.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.tgx.chess.spring.auth.repository.ProfileRepository;
import com.tgx.chess.spring.biz.bill.pay.model.BillEntity;
import com.tgx.chess.spring.biz.bill.pay.model.ItemEntity;
import com.tgx.chess.spring.biz.bill.pay.repository.BillRepository;
import com.tgx.chess.spring.device.model.ClientEntity;
import com.tgx.chess.spring.device.model.DeviceEntity;
import com.tgx.chess.spring.device.repository.ClientRepository;
import com.tgx.chess.spring.device.repository.DeviceRepository;
/**
 * @author william.d.zk
 */
@Service
public class BillService
{
    private final BillRepository    _BillRepository;
    private final ProfileRepository _ProfileRepository;
    private final ClientRepository  _ClientRepository;
    private final DeviceRepository  _DeviceRepository;

    @Autowired
    public BillService(BillRepository billRepository,
                       ProfileRepository profileRepository,
                       ClientRepository clientRepository,
                       DeviceRepository deviceRepository)
    {
        _BillRepository = billRepository;
        _ProfileRepository = profileRepository;
        _ClientRepository = clientRepository;
        _DeviceRepository = deviceRepository;
    }

    public Optional<DeviceEntity> findDeviceByMac(String mac)
    {
        return Optional.ofNullable(_DeviceRepository.findByMac(mac));
    }

    public Optional<ClientEntity> findClientByName(String name)
    {
        return Optional.ofNullable(_ClientRepository.findByAuth(name));
    }

    public Optional<BillEntity> findByBill(String bill)
    {
        return Optional.ofNullable(_BillRepository.findByBill(bill));
    }

    public List<BillEntity> findAllByMac(String mac)
    {
        return _BillRepository.findAllByMac(mac);
    }

    public BillEntity findLastByMacAndItem(String mac, ItemEntity item)
    {
        return _BillRepository.findFirstByMacAndItem(mac,
                                                     item,
                                                     Sort.by("updatedAt")
                                                         .descending());
    }

    public BillEntity saveBill(BillEntity bill)
    {
        return _BillRepository.save(bill);
    }

}
