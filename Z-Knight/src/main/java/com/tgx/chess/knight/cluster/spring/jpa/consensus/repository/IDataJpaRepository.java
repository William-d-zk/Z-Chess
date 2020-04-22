package com.tgx.chess.knight.cluster.spring.jpa.consensus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tgx.chess.knight.cluster.spring.jpa.consensus.dao.ConsistentEntity;

/**
 * @author william.d.zk
 * @date 2020/4/23
 */
@Repository
public interface IDataJpaRepository
        extends
        JpaRepository<ConsistentEntity,
                      Long>
{

}
