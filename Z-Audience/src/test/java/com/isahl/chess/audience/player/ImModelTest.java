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

package com.isahl.chess.audience.player;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.audience.testing.BaseTest;
import com.isahl.chess.player.domain.*;
import org.junit.jupiter.api.Test;

public class ImModelTest extends BaseTest {

  @Test
  void testUser() {
    User user = new User("testuser", "hash123", "Test User");
    assertEquals("testuser", user.getUsername());
    assertEquals("hash123", user.getPasswordHash());
    assertEquals("Test User", user.getDisplayName());
    assertFalse(user.getOnline());

    user.setEmail("test@example.com");
    assertEquals("test@example.com", user.getEmail());

    user.setPhone("1234567890");
    assertEquals("1234567890", user.getPhone());

    user.setOnline(true);
    assertTrue(user.getOnline());
  }

  @Test
  void testGroup() {
    Group group = new Group("Test Group", 1L);
    assertEquals("Test Group", group.getName());
    assertEquals(1L, group.getOwnerId());
    assertTrue(group.getActive());
    assertNotNull(group.getMembers());

    group.setDescription("A test group");
    assertEquals("A test group", group.getDescription());

    group.setActive(false);
    assertFalse(group.getActive());
  }

  @Test
  void testGroupMember() {
    GroupMember member = new GroupMember(1L, "ADMIN");
    assertEquals(1L, member.getUserId());
    assertEquals("ADMIN", member.getRole());

    member.setRole("MEMBER");
    assertEquals("MEMBER", member.getRole());
  }

  @Test
  void testGroupMemberAssociation() {
    Group group = new Group("Test Group", 1L);
    GroupMember member = new GroupMember(2L, "MEMBER");

    group.addMember(member);
    assertEquals(group, member.getGroup());
    assertTrue(group.getMembers().contains(member));
    assertEquals(1, group.getMembers().size());

    group.removeMember(member);
    assertNull(member.getGroup());
    assertTrue(group.getMembers().isEmpty());
  }

  @Test
  void testMessage() {
    Message message = new Message(1L, "Hello World", "TEXT");
    assertEquals(1L, message.getSenderId());
    assertEquals("Hello World", message.getContent());
    assertEquals("TEXT", message.getType());
    assertFalse(message.getDelivered());
    assertFalse(message.getRecalled());

    message.setGroupId(100L);
    assertEquals(100L, message.getGroupId());

    message.setReceiverId(2L);
    assertEquals(2L, message.getReceiverId());

    message.setSequenceNum(1L);
    assertEquals(1L, message.getSequenceNum());

    message.setDelivered(true);
    assertTrue(message.getDelivered());

    message.setRecalled(true);
    assertTrue(message.getRecalled());
  }

  @Test
  void testUserSession() {
    UserSession session = new UserSession(1L, "token123", "WEB", "192.168.1.1");
    assertEquals(1L, session.getUserId());
    assertEquals("token123", session.getSessionToken());
    assertEquals("WEB", session.getClientType());
    assertEquals("192.168.1.1", session.getLoginIp());
    assertTrue(session.getActive());
    assertNotNull(session.getLoginTime());
    assertNull(session.getLogoutTime());

    session.setClientVersion("1.0.0");
    assertEquals("1.0.0", session.getClientVersion());

    session.setLogoutTime(System.currentTimeMillis());
    assertNotNull(session.getLogoutTime());

    session.setActive(false);
    assertFalse(session.getActive());
  }
}
