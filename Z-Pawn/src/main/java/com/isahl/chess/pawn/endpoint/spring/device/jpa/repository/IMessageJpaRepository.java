/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.pawn.endpoint.spring.device.jpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.isahl.chess.pawn.endpoint.spring.device.jpa.model.MessageEntity;
import com.isahl.chess.pawn.spring.jpa.BaseRepository;

/**
 * @author william.d.zk
 * 
 * @date 2019-07-31
 */
@Repository
public interface IMessageJpaRepository
        extends
        BaseRepository<MessageEntity>
{

    MessageEntity findByOriginAndDestinationAndMsgId(long origin, long destination, long msgId);

    MessageEntity findByOriginAndDestinationAndMsgIdAndCreatedAtAfter(long origin,
                                                                      long destination,
                                                                      long msgId,
                                                                      LocalDateTime time);

    List<MessageEntity> findAllByOriginAndDestinationAndMsgId(long origin, long destination, long msgId);

    List<MessageEntity> findAllByOriginOrDestination(long origin, long destination);

    List<MessageEntity> findAllByOriginAndMsgIdAndDirectionAndOwner(long origin,
                                                                    long msgId,
                                                                    String direction,
                                                                    String owner);

    List<MessageEntity> findAllByDestinationAndMsgIdAndDirectionAndOwner(long destination,
                                                                         long msgId,
                                                                         String direction,
                                                                         String owner);

    List<MessageEntity> findAllByDestinationAndMsgIdBefore(long destination, long msgId);

    List<MessageEntity> findAllByOriginAndMsgIdBefore(long origin, long msgId);

    List<MessageEntity> findAllByOriginAndMsgIdAfter(long origin, long msgId);

    @Query(value = "select * from \"isahl.z-chess\".message m where m.payload->>'topic'=:p_topic limit :p_limit order by desc ",
           nativeQuery = true)
    List<MessageEntity> listByTopic(@Param("p_topic") String topic, @Param("p_limit") int limit);
}
