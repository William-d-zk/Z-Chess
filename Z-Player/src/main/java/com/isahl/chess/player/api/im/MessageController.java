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

package com.isahl.chess.player.api.im;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.player.domain.Message;
import com.isahl.chess.player.domain.User;
import com.isahl.chess.player.repository.MessageRepository;
import com.isahl.chess.player.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/im/messages")
public class MessageController
{
    private static final Logger _Logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageRepository _MessageRepository;
    private final UserRepository _UserRepository;

    @Autowired
    public MessageController(MessageRepository messageRepository, UserRepository userRepository)
    {
        _MessageRepository = messageRepository;
        _UserRepository = userRepository;
    }

    @PostMapping
    public ZResponse<?> sendMessage(@RequestBody SendMessageRequest request, @RequestHeader("X-User-Id") Long senderId)
    {
        Message message = new Message(senderId, request.content, request.type);
        message.setGroupId(request.groupId);
        message.setReceiverId(request.receiverId);
        Long maxSeq = _MessageRepository.findMaxSequenceNumByGroupId(request.groupId);
        message.setSequenceNum(maxSeq != null ? maxSeq + 1 : 1);
        message = _MessageRepository.save(message);
        _Logger.info("Message sent: id={}, groupId={}, senderId={}", message.getId(), request.groupId, senderId);
        return ZResponse.success(Map.of(
                "messageId", message.getId(),
                "sequenceNum", message.getSequenceNum()
        ));
    }

    @GetMapping("group/{groupId}")
    public ZResponse<?> getGroupMessages(@PathVariable Long groupId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size)
    {
        Page<Message> messages = _MessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(page, size));
        List<Map<String, Object>> result = messages.getContent().stream()
                .filter(m -> !m.getRecalled())
                .map(m -> {
                    Optional<User> senderOpt = _UserRepository.findById(m.getSenderId());
                    String senderName = senderOpt.map(User::getDisplayName).orElse("Unknown");
                    return Map.<String, Object>of(
                            "messageId", m.getId(),
                            "senderId", m.getSenderId(),
                            "senderName", senderName,
                            "content", m.getContent(),
                            "type", m.getType(),
                            "sequenceNum", m.getSequenceNum(),
                            "createdAt", m.getCreatedAt().toString()
                    );
                })
                .collect(Collectors.toList());
        return ZResponse.success(Map.of(
                "messages", result,
                "page", page,
                "size", size,
                "totalPages", messages.getTotalPages()
        ));
    }

    @GetMapping("offline")
    public ZResponse<?> getOfflineMessages(@RequestHeader("X-User-Id") Long userId)
    {
        List<Message> messages = _MessageRepository.findByReceiverIdAndDeliveredFalse(userId);
        List<Map<String, Object>> result = messages.stream()
                .map(m -> {
                    Optional<User> senderOpt = _UserRepository.findById(m.getSenderId());
                    String senderName = senderOpt.map(User::getDisplayName).orElse("Unknown");
                    return Map.<String, Object>of(
                            "messageId", m.getId(),
                            "senderId", m.getSenderId(),
                            "senderName", senderName,
                            "content", m.getContent(),
                            "type", m.getType(),
                            "createdAt", m.getCreatedAt().toString()
                    );
                })
                .collect(Collectors.toList());
        messages.forEach(m -> {
            m.setDelivered(true);
            _MessageRepository.save(m);
        });
        return ZResponse.success(result);
    }

    @PostMapping("{messageId}/delivered")
    public ZResponse<?> markDelivered(@PathVariable Long messageId)
    {
        Optional<Message> msgOpt = _MessageRepository.findById(messageId);
        if(msgOpt.isEmpty()) {
            return ZResponse.error("Message not found");
        }
        Message message = msgOpt.get();
        message.setDelivered(true);
        _MessageRepository.save(message);
        return ZResponse.success("Message marked as delivered");
    }

    @DeleteMapping("{messageId}")
    public ZResponse<?> recallMessage(@PathVariable Long messageId, @RequestHeader("X-User-Id") Long userId)
    {
        Optional<Message> msgOpt = _MessageRepository.findById(messageId);
        if(msgOpt.isEmpty()) {
            return ZResponse.error("Message not found");
        }
        Message message = msgOpt.get();
        if(!message.getSenderId().equals(userId)) {
            return ZResponse.error("Can only recall own messages");
        }
        message.setRecalled(true);
        _MessageRepository.save(message);
        _Logger.info("Message recalled: id={}, senderId={}", messageId, userId);
        return ZResponse.success("Message recalled");
    }

    public static class SendMessageRequest
    {
        public Long groupId;
        public Long receiverId;
        public String content;
        public String type = "TEXT";
    }
}
