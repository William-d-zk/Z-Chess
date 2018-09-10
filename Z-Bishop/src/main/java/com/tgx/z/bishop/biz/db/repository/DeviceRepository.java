package com.tgx.z.bishop.biz.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tgx.z.bishop.biz.db.dto.Device;

@Repository
public interface DeviceRepository
        extends
        JpaRepository<Device, Long>
{
    Device findBySn(String sn);
}
