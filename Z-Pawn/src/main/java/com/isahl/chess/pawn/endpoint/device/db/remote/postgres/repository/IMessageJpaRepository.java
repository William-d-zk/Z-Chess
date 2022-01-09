/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.db.remote.postgres.repository;

import com.isahl.chess.pawn.endpoint.device.db.remote.postgres.model.MessageEntity;
import com.isahl.chess.rook.storage.db.repository.BaseLongRepository;
import org.springframework.stereotype.Repository;

/**
 * @author william.d.zk
 * @date 2019-07-31
 */
@Repository("remote-message-jpa-repository")
public interface IMessageJpaRepository
        extends BaseLongRepository<MessageEntity>
{
   /*
    List<MessageEntity> findAllByOriginAndDestinationAndTopic(long origin, long destination, String topic);

    @Query(value = "select * from \"z-chess\".message m where m.body->>'topic'=:p_topic order by id desc limit :p_limit",
           nativeQuery = true)
    List<MessageEntity> listByTopic(
            @Param("p_topic")
                    String topic,
            @Param("p_limit")
                    int limit);


    @Transactional
    @Modifying
    @Query(value = "delete from \"z-chess\".message m where m.destination=:p_dest and m.owner=:p_owner",
           nativeQuery = true)
    void deleteAllByDestination(
            @Param("p_dest")
                    long destination,
            @Param("p_owner")
                    String owner);

     */
}
