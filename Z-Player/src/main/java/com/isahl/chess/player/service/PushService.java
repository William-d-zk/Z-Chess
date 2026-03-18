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

package com.isahl.chess.player.service;

import com.isahl.chess.player.domain.GroupMember;
import com.isahl.chess.player.domain.Message;
import com.isahl.chess.player.domain.User;
import com.isahl.chess.player.repository.GroupMemberRepository;
import com.isahl.chess.player.repository.MessageRepository;
import com.isahl.chess.player.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PushService
{
    private static final Logger _Logger = LoggerFactory.getLogger(PushService.class);

    private final MessageRepository _MessageRepository;
    private final UserRepository _UserRepository;
    private final GroupMemberRepository _GroupMemberRepository;
    private final SimpMessagingTemplate _MessagingTemplate;

    @Autowired
    public PushService(MessageRepository messageRepository, UserRepository userRepository, GroupMemberRepository groupMemberRepository, SimpMessagingTemplate messagingTemplate)
    {
        _MessageRepository = messageRepository;
        _UserRepository = userRepository;
        _GroupMemberRepository = groupMemberRepository;
        _MessagingTemplate = messagingTemplate;
    }

    public void pushMessage(Message message)
    {
        String destination = message.getGroupId() != null
                ? "/topic/group." + message.getGroupId()
                : "/queue/user." + message.getReceiverId();
        _MessagingTemplate.convertAndSend(destination, message);
        _Logger.debug("Message pushed: destination={}, messageId={}", destination, message.getId());
    }

    public void pushToUser(Long userId, Object payload)
    {
        String destination = "/queue/user." + userId;
        _MessagingTemplate.convertAndSend(destination, payload);
        _Logger.debug("Pushed to user: userId={}, destination={}", userId, destination);
    }

    public void pushToGroup(Long groupId, Object payload)
    {
        String destination = "/topic/group." + groupId;
        _MessagingTemplate.convertAndSend(destination, payload);
        _Logger.debug("Pushed to group: groupId={}, destination={}", groupId, destination);
    }

    public void notifyUserStatusChange(Long userId, boolean online)
    {
        User user = _UserRepository.findById(userId).orElse(null);
        if(user == null) {
            return;
        }
        user.setOnline(online);
        _UserRepository.save(user);
        List<GroupMember> memberships = _GroupMemberRepository.findByUserId(userId);
        for(GroupMember member : memberships) {
            pushToGroup(member.getGroup().getId(), Map.of("type", "USER_STATUS", "userId", userId, "online", online));
        }
    }

}