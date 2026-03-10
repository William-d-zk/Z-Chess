# Docker 运行模式集群配置检查报告

## 一、集群架构概述

Z-Chess 在 Docker 模式下采用 **3 节点 Raft 集群** 架构，包含以下组件：

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker 集群架构                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   raft00    │  │   raft01    │  │   raft02    │         │
│  │  (Leader)   │  │  (Follower) │  │  (Follower) │         │
│  │ 172.30.10.110│  │ 172.30.10.111│  │ 172.30.10.112│        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │
│         │                │                │                 │
│         └────────────────┼────────────────┘                 │
│                          │                                  │
│                   ┌──────┴──────┐                          │
│                   │   db-pg     │                          │
│                   │ 172.30.10.254│                          │
│                   │ PostgreSQL 17│                          │
│                   └─────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、节点配置详情

### 2.1 节点网络配置

| 节点 | 容器名 | 主机名 | Endpoint 网络 | Gate 网络 | 开放端口 |
|------|--------|--------|---------------|-----------|----------|
| raft00 | raft00 | raft00 | 172.30.10.110 | 172.30.11.110 | 8080, 8000, 1883 |
| raft01 | raft01 | raft01 | 172.30.10.111 | - | 1884, 8100 |
| raft02 | raft02 | raft02 | 172.30.10.112 | - | 1885, 8200 |
| db-pg | db-pg | db-pg | 172.30.10.254 | - | 5432 |

### 2.2 Host 映射配置（extra_hosts）

所有节点都配置了以下 hosts：

```yaml
extra_hosts:
  - "raft00:172.30.10.110"
  - "raft01:172.30.10.111"
  - "raft02:172.30.10.112"
  - "gate00:172.30.11.110"    # raft00 的网关地址
  - "db-pg.isahl.com:172.30.10.254"
```

---

## 三、Raft 集群关键配置

### 3.1 集群通信参数

```properties
# 集群网络配置
z.chess.pawn.io.cluster.keep_alive=true
z.chess.pawn.io.cluster.connect_timeout_in_second=1S
z.chess.pawn.io.cluster.write_timeout_in_second=5S
z.chess.pawn.io.cluster.read_timeout_in_minute=1M
z.chess.pawn.io.cluster.so_linger_in_second=30S
z.chess.pawn.io.cluster.send_buffer_size=64KB
z.chess.pawn.io.cluster.recv_buffer_size=64KB
z.chess.pawn.io.cluster.send_queue_max=64
z.chess.pawn.io.cluster.tcp_no_delay=true
```

### 3.2 集群节点发现

根据 `RaftPeer.java` 代码分析，集群节点通过以下方式发现：

```java
// 从 _RaftConfig.getPeers() 获取议会成员
// 从 _RaftConfig.getNodes() 获取集群节点成员
graphUp(_SelfGraph.getPeers(), _RaftConfig.getNodes());
```

节点连接策略：
- **仅连接 peer < self 的节点**（避免重复连接）
- 使用 ZChat 协议进行集群通信
- 自动重连机制

### 3.3 服务依赖关系

```yaml
# raft00 无额外依赖（作为主节点先启动）

# raft01 和 raft02 依赖：
depends_on:
  db-pg:
    condition: service_healthy    # 等待数据库健康
  raft10:
    condition: service_started    # 等待 raft00 启动
```

---

## 四、网络配置详情

### 4.1 Docker 网络配置

```yaml
networks:
  endpoint:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.10.0/24    # 256 个 IP
          gateway: 172.30.10.1
  
  gate:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.11.0/24    # 仅 raft00 使用
          gateway: 172.30.11.1
```

### 4.2 网络隔离说明

| 网络 | 用途 | 访问范围 |
|------|------|----------|
| endpoint | 集群内部通信 | 所有节点互通 |
| gate | 网关/外部通信 | 仅 raft00 接入 |

---

## 五、数据持久化配置

### 5.1 挂载卷配置

```yaml
volumes:
  # 节点数据持久化
  - ~/Z-Chess/raft00:/root/Z-Chess      # Raft 数据/WAL
  - ~/Z-Chess/logs/raft00:/app/logs     # 应用日志
  
  # PostgreSQL 数据
  - ~/Services/db/postgresql17:/var/lib/postgresql/data
  - ~/Services/db/postgresql17/log:/var/lib/postgresql/log
```

### 5.2 数据目录说明

| 目录 | 内容 | 备注 |
|------|------|------|
| `~/Z-Chess/raft00` | Raft 日志、快照、状态机数据 | 需定期备份 |
| `~/Z-Chess/logs/raft00` | 应用运行日志 | 可配置日志轮转 |
| `~/Services/db/postgresql17` | 业务数据 | PostgreSQL 17 数据 |

---

## 六、健康检查配置

### 6.1 PostgreSQL 健康检查

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d postgres"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

### 6.2 Java 应用健康检查（Dockerfile 中配置）

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

---

## 七、环境变量配置

### 7.1 数据库连接

```yaml
environment:
  - DB_HOST=db-pg.isahl.com    # 或 db-pg (amd64)
  - DB_PORT=5432
  - DB_NAME=zchess
  - POSTGRES_USER=postgres
  - POSTGRES_PASSWORD=postgres
```

### 7.2 JVM 参数

```yaml
environment:
  - JAVA_OPTS=-XX:+UseContainerSupport 
                -XX:MaxRAMPercentage=75.0 
                -XX:InitialRAMPercentage=50.0
  - SPRING_PROFILES_ACTIVE=online  # aarch64
  # - SPRING_PROFILES_ACTIVE=local   # amd64
```

---

## 八、集群启动顺序

```
1. db-pg (PostgreSQL)
   └─ 等待健康检查通过
   
2. raft00 (主节点)
   └─ 等待 db-pg 健康
   └─ 初始化 Raft 状态机
   └─ 可能触发 Leader 选举
   
3. raft01 (Follower)
   └─ 等待 raft00 启动
   └─ 加入集群
   
4. raft02 (Follower)
   └─ 等待 raft00 启动
   └─ 加入集群
```

---

## 九、潜在问题检查

### 9.1 ⚠️ 发现的问题

| 问题 | 严重程度 | 说明 | 建议 |
|------|----------|------|------|
| **密码硬编码** | 中 | docker-compose 中明文密码 | 使用 Docker Secrets 或环境变量文件 |
| **amd64 密码不一致** | 高 | amd64 使用 `postgress`（拼写错误？）| 统一为 `postgres` |
| **无资源限制** | 中 | Java 应用无内存限制 | 添加 `mem_limit` 配置 |
| **日志未轮转** | 低 | 依赖 docker 日志驱动 | 确认 `max-file` 配置有效 |
| **缺少重启策略** | 低 | raft01/raft02 未配置 restart | 添加 `unless-stopped` |

### 9.2 配置差异（aarch64 vs amd64）

| 配置项 | aarch64 | amd64 | 建议 |
|--------|---------|-------|------|
| 数据库域名 | `db-pg.isahl.com` | `db-pg` | 统一使用域名 |
| 数据库密码 | `postgres` | `postgress` | 修正拼写 |
| 配置文件 | `online` | `local` | 生产环境统一 `online` |
| 卷挂载 | 有 logs 卷 | 无 logs 卷 | 统一添加日志卷 |

---

## 十、优化建议

### 10.1 推荐配置调整

```yaml
# 添加资源限制
services:
  raft10:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
    restart: unless-stopped    # 添加重启策略
    
  raft11:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 2G
    restart: unless-stopped
    # ... 其他节点类似
```

### 10.2 使用 Docker Secrets（生产环境）

```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt

services:
  db-pg:
    environment:
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_password
```

### 10.3 集群监控建议

```yaml
# 添加 Prometheus 监控标签
labels:
  - "prometheus.io/scrape=true"
  - "prometheus.io/port=8080"
  - "prometheus.io/path=/actuator/prometheus"
```

---

## 十一、验证检查清单

- [ ] 所有节点 hosts 配置正确（`extra_hosts`）
- [ ] 网络子网无冲突（172.30.10.0/24, 172.30.11.0/24）
- [ ] 数据目录已创建（`~/Z-Chess/raft0x`, `~/Services/db/postgresql17`）
- [ ] 权限正确（postgres 目录权限 999:999）
- [ ] 防火墙开放端口（5432, 8080, 8000, 1883-1885）
- [ ] 时区配置一致（Asia/Shanghai）
- [ ] 健康检查 endpoint 可用（/actuator/health）

---

*报告生成时间: 2025-01*
*适用于: Z-Chess Docker 部署 (PostgreSQL 17 + Alpine 3.20)*
