# Phase 4: IM基础服务

## Goal

构建IM核心能力：认证、群组、消息推送

## Requirements

- **IM-01**: 用户认证 — 员工身份验证（内部SSO/LDAP集成）
- **IM-02**: 群组聊天 — 群组对话、群管理（创建/解散/成员管理）
- **IM-03**: 消息推送 — 在线消息实时推送，离线消息持久化

## Success Criteria

1. 员工可通过内部SSO/LDAP登录
2. 用户可创建/解散群组、管理群成员
3. 消息实时推送，延迟 < 500ms
4. 离线消息可持久化，用户上线后收到
5. 群组消息顺序一致

## Implementation

### 4.1 Domain Models

```
com.isahl.chess.queen.domain
├── User.java              # 用户实体
├── Group.java             # 群组实体
├── GroupMember.java       # 群成员关联
└── Message.java           # 消息实体
```

### 4.2 Repositories

```
com.isahl.chess.queen.repository
├── UserRepository.java
├── GroupRepository.java
├── GroupMemberRepository.java
└── MessageRepository.java
```

### 4.3 Services

```
com.isahl.chess.queen.service
├── AuthService.java       # 认证服务 (IM-01)
├── GroupService.java      # 群组服务 (IM-02)
├── MessageService.java    # 消息服务 (IM-03)
└── PushService.java       # 推送服务
```

### 4.4 API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/im/auth/login | 用户登录 |
| POST | /api/im/auth/logout | 用户登出 |
| GET | /api/im/users/{id} | 获取用户信息 |
| POST | /api/im/groups | 创建群组 |
| DELETE | /api/im/groups/{id} | 解散群组 |
| POST | /api/im/groups/{id}/members | 添加成员 |
| DELETE | /api/im/groups/{id}/members/{userId} | 移除成员 |
| GET | /api/im/groups/{id}/messages | 获取群消息历史 |
| POST | /api/im/messages | 发送消息 |
| WS | /ws/im | WebSocket端点 |

### 4.5 Dependencies

Add to Z-Queen pom.xml:
- spring-boot-starter-web
- spring-boot-starter-websocket
- spring-boot-starter-data-jpa
- postgresql
- spring-boot-starter-validation

## Files to Create

1. `Z-Queen/src/main/java/com/isahl/chess/queen/domain/*.java` (4 files)
2. `Z-Queen/src/main/java/com/isahl/chess/queen/repository/*.java` (4 files)
3. `Z-Queen/src/main/java/com/isahl/chess/queen/service/*.java` (4 files)
4. `Z-Queen/src/main/java/com/isahl/chess/queen/api/*.java` (REST controllers)
5. `Z-Queen/src/main/java/com/isahl/chess/queen/config/*Config.java` (WebSocket, Security)
6. `Z-Queen/src/main/resources/application.properties`

## Verification

```bash
mvn compile -pl Z-Queen -am
mvn test -pl Z-Queen
```
