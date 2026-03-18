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
import com.isahl.chess.player.domain.Group;
import com.isahl.chess.player.domain.GroupMember;
import com.isahl.chess.player.domain.User;
import com.isahl.chess.player.repository.GroupMemberRepository;
import com.isahl.chess.player.repository.GroupRepository;
import com.isahl.chess.player.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/im/groups")
public class GroupController
{
    private static final Logger _Logger = LoggerFactory.getLogger(GroupController.class);

    private final GroupRepository _GroupRepository;
    private final GroupMemberRepository _MemberRepository;
    private final UserRepository _UserRepository;

    @Autowired
    public GroupController(GroupRepository groupRepository, GroupMemberRepository memberRepository, UserRepository userRepository)
    {
        _GroupRepository = groupRepository;
        _MemberRepository = memberRepository;
        _UserRepository = userRepository;
    }

    @PostMapping
    public ZResponse<?> createGroup(@RequestBody CreateGroupRequest request, @RequestHeader("X-User-Id") Long userId)
    {
        Group group = new Group(request.name, userId);
        group.setDescription(request.description);
        group = _GroupRepository.save(group);
        GroupMember ownerMember = new GroupMember(userId, "OWNER");
        group.addMember(ownerMember);
        _GroupRepository.save(group);
        _Logger.info("Group created: id={}, name={}, ownerId={}", group.getId(), group.getName(), userId);
        return ZResponse.success(Map.of(
                "groupId", group.getId(),
                "name", group.getName()
        ));
    }

    @DeleteMapping("{groupId}")
    public ZResponse<?> dissolveGroup(@PathVariable Long groupId, @RequestHeader("X-User-Id") Long userId)
    {
        Optional<Group> groupOpt = _GroupRepository.findById(groupId);
        if(groupOpt.isEmpty()) {
            return ZResponse.error("Group not found");
        }
        Group group = groupOpt.get();
        if(!group.getOwnerId().equals(userId)) {
            return ZResponse.error("Only owner can dissolve the group");
        }
        group.setActive(false);
        _GroupRepository.save(group);
        _Logger.info("Group dissolved: id={}", groupId);
        return ZResponse.success("Group dissolved");
    }

    @GetMapping
    public ZResponse<?> listGroups(@RequestHeader("X-User-Id") Long userId)
    {
        List<Group> groups = _GroupRepository.findByMemberUserId(userId);
        List<Map<String, Object>> result = groups.stream()
                .map(g -> Map.<String, Object>of(
                        "groupId", g.getId(),
                        "name", g.getName(),
                        "description", g.getDescription() != null ? g.getDescription() : "",
                        "memberCount", g.getMembers().size()
                ))
                .collect(Collectors.toList());
        return ZResponse.success(result);
    }

    @GetMapping("{groupId}")
    public ZResponse<?> getGroup(@PathVariable Long groupId)
    {
        Optional<Group> groupOpt = _GroupRepository.findById(groupId);
        if(groupOpt.isEmpty()) {
            return ZResponse.error("Group not found");
        }
        Group group = groupOpt.get();
        List<GroupMember> members = _MemberRepository.findByGroupId(groupId);
        List<Map<String, Object>> memberList = members.stream()
                .map(m -> {
                    Optional<User> userOpt = _UserRepository.findById(m.getUserId());
                    String displayName = userOpt.map(User::getDisplayName).orElse("Unknown");
                    return Map.<String, Object>of(
                            "userId", m.getUserId(),
                            "displayName", displayName,
                            "role", m.getRole()
                    );
                })
                .collect(Collectors.toList());
        return ZResponse.success(Map.of(
                "groupId", group.getId(),
                "name", group.getName(),
                "description", group.getDescription() != null ? group.getDescription() : "",
                "ownerId", group.getOwnerId(),
                "active", group.getActive(),
                "members", memberList
        ));
    }

    @PostMapping("{groupId}/members")
    public ZResponse<?> addMember(@PathVariable Long groupId, @RequestBody AddMemberRequest request, @RequestHeader("X-User-Id") Long userId)
    {
        Optional<Group> groupOpt = _GroupRepository.findById(groupId);
        if(groupOpt.isEmpty()) {
            return ZResponse.error("Group not found");
        }
        Group group = groupOpt.get();
        Optional<User> userOpt = _UserRepository.findByUsername(request.username);
        if(userOpt.isEmpty()) {
            return ZResponse.error("User not found");
        }
        Long targetUserId = userOpt.get().getId();
        if(_MemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            return ZResponse.error("User already in group");
        }
        GroupMember member = new GroupMember(targetUserId, "MEMBER");
        group.addMember(member);
        _GroupRepository.save(group);
        _Logger.info("Member added: groupId={}, userId={}", groupId, targetUserId);
        return ZResponse.success(Map.of("userId", targetUserId, "role", "MEMBER"));
    }

    @DeleteMapping("{groupId}/members/{targetUserId}")
    public ZResponse<?> removeMember(@PathVariable Long groupId, @PathVariable Long targetUserId, @RequestHeader("X-User-Id") Long userId)
    {
        Optional<Group> groupOpt = _GroupRepository.findById(groupId);
        if(groupOpt.isEmpty()) {
            return ZResponse.error("Group not found");
        }
        Group group = groupOpt.get();
        if(!group.getOwnerId().equals(userId) && !userId.equals(targetUserId)) {
            return ZResponse.error("No permission to remove member");
        }
        Optional<GroupMember> memberOpt = _MemberRepository.findByGroupIdAndUserId(groupId, targetUserId);
        if(memberOpt.isEmpty()) {
            return ZResponse.error("Member not in group");
        }
        _MemberRepository.delete(memberOpt.get());
        _Logger.info("Member removed: groupId={}, userId={}", groupId, targetUserId);
        return ZResponse.success("Member removed");
    }

    public static class CreateGroupRequest
    {
        public String name;
        public String description;
    }

    public static class AddMemberRequest
    {
        public String username;
    }
}
