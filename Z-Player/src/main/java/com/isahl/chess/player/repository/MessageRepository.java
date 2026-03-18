/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.player.repository;

import com.isahl.chess.player.domain.Message;
import com.isahl.chess.rook.storage.db.repository.BaseLongRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository
        extends BaseLongRepository<Message>
{
    Page<Message> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    Page<Message> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    Page<Message> findByReceiverIdAndDeliveredFalseOrderByCreatedAtAsc(Long receiverId, Pageable pageable);

    @Query("SELECT MAX(m.sequenceNum) FROM Message m WHERE m.groupId = :groupId")
    Long findMaxSequenceNumByGroupId(Long groupId);

    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.createdAt > :since ORDER BY m.createdAt ASC")
    List<Message> findByGroupIdSince(Long groupId, LocalDateTime since);

    List<Message> findByReceiverIdAndDeliveredFalse(Long receiverId);
}
