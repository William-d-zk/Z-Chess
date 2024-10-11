package com.isahl.chess.pawn.endpoint.device.db.central.repository;

import com.isahl.chess.pawn.endpoint.device.db.central.model.LcOrderEntity;
import com.isahl.chess.rook.storage.db.repository.BaseLongRepository;
import org.springframework.stereotype.Repository;

/**
 * 良仓api 相关数据写入
 *
 * @author xiaojiang.lxj at 2024-09-24 11:24.
 */
@Repository
public interface ILcApiRepository extends BaseLongRepository<LcOrderEntity> {

}
