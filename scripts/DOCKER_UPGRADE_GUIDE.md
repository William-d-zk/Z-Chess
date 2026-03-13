# Z-Chess Docker 升级指南

## 升级概述

本次升级将 PostgreSQL 升级到 17 版本，同时升级 Linux 内核/基础镜像。

## 主要变更

### 1. PostgreSQL 升级

| 组件 | 旧版本 | 新版本 | 说明 |
|------|--------|--------|------|
| PostgreSQL | 未指定/旧版 | **17.2** | 最新稳定版 |
| Alpine Linux | 3.18 | **3.20** | 内核 6.6.x |
| 镜像名称 | `isahl/postgres` | `isahl/postgres:17.2-{arch}` | 带版本标签 |

### 2. 应用镜像升级

| 组件 | 旧版本 | 新版本 | 说明 |
|------|--------|--------|------|
| 基础镜像 | Alpine 3.18 | **Alpine 3.20** | 安全更新 |
| JDK | 17 (旧构建) | **17.0.12+10** | Liberica JDK |
| 镜像标签 | `aarch64/amd64` | `:2.0` | 版本化管理 |

## PostgreSQL 17 新特性

### 性能提升
- **VACUUM 性能改进**：更快的并发清理
- **WAL 日志优化**：减少 I/O 开销
- **并行查询增强**：支持更多并行操作
- **JIT 编译改进**：更快的表达式求值

### 新功能
- **JSON 性能提升**：`JSONB` 操作更快
- **逻辑复制增强**：支持更多数据类型
- **SQL/JSON 标准**：新增 `JSON_SERIALIZE`, `JSON_SCALAR` 函数

### 兼容性注意
- PostgreSQL 17 不再支持旧版客户端（< 9.6）
- 部分配置参数名称变更
- 数据目录格式不向下兼容

## 升级步骤

### 1. 备份数据（重要！）

```bash
# 停止现有容器
docker-compose down

# 备份 PostgreSQL 数据目录
cp -r ~/Services/db/postgresql ~/Services/db/postgresql-backup-$(date +%Y%m%d)

# 导出数据库（使用与 application.properties 一致的用户名）
docker exec db-pg pg_dumpall -U chess > zchess-backup.sql
```

### 2. 更新目录结构

```bash
# 创建 PostgreSQL 17 数据目录
mkdir -p ~/Services/db/postgresql17/log

# 设置权限
sudo chown -R 999:999 ~/Services/db/postgresql17
```

### 3. 数据迁移（pg_upgrade）

```bash
# 使用临时容器进行升级
docker run --rm \
  -v ~/Services/db/postgresql:/var/lib/postgresql/14/data \
  -v ~/Services/db/postgresql17:/var/lib/postgresql/17/data \
  isahl/postgres:17.2-amd64 \
  pg_upgrade \
    --old-bindir=/usr/lib/postgresql/14/bin \
    --new-bindir=/usr/lib/postgresql/17/bin \
    --old-datadir=/var/lib/postgresql/14/data \
    --new-datadir=/var/lib/postgresql/17/data \
    --check

# 执行升级
docker run --rm \
  -v ~/Services/db/postgresql:/var/lib/postgresql/14/data \
  -v ~/Services/db/postgresql17:/var/lib/postgresql/17/data \
  isahl/postgres:17.2-amd64 \
  pg_upgrade \
    --old-bindir=/usr/lib/postgresql/14/bin \
    --new-bindir=/usr/lib/postgresql/17/bin \
    --old-datadir=/var/lib/postgresql/14/data \
    --new-datadir=/var/lib/postgresql/17/data
```

### 4. 启动新环境

```bash
# ARM64
cd docker/aarch64
docker-compose up -d

# AMD64
cd docker/amd64
docker-compose up -d
```

### 5. 验证升级

```bash
# 检查 PostgreSQL 版本（使用与配置一致的用户名）
docker exec db-pg psql -U chess -d isahl_9.x -c "SELECT version();"

# 预期输出：PostgreSQL 17.2 on ...

# 检查数据库状态
docker exec db-pg pg_isready -U chess -d isahl_9.x

# 查看应用日志
docker logs -f raft00
```

## 快速开始（新安装）

如果不需要保留旧数据，可以直接使用新配置：

```bash
# 清理旧数据（警告：这会删除所有数据！）
rm -rf ~/Services/db/postgresql
mkdir -p ~/Services/db/postgresql17/log

# 构建并启动
cd docker/aarch64  # 或 amd64
docker-compose up --build -d
```

## 配置优化

### PostgreSQL 17 推荐配置

根据硬件资源调整 `scripts/postgres17/postgresql.conf`：

```ini
# 4GB 内存示例
shared_buffers = 1GB
effective_cache_size = 3GB
work_mem = 16MB
maintenance_work_mem = 256MB
max_connections = 500
```

### JVM 参数调优

在 `docker-compose.yaml` 中调整：

```yaml
environment:
  - JAVA_OPTS=-XX:+UseContainerSupport 
                -XX:MaxRAMPercentage=75.0
                -XX:+UseG1GC
                -XX:MaxGCPauseMillis=200
```

## 故障排查

### 问题：无法连接到 PostgreSQL

```bash
# 检查容器状态
docker-compose ps

# 查看 PostgreSQL 日志
docker logs db-pg

# 检查端口
docker exec db-pg ss -tlnp | grep 5432
```

### 问题：数据目录权限错误

```bash
# 修复权限
sudo chown -R 999:999 ~/Services/db/postgresql17
sudo chmod 700 ~/Services/db/postgresql17
```

### 问题：升级后数据不兼容

```bash
# 回滚到旧版本
docker-compose down
cp -r ~/Services/db/postgresql-backup-xxx ~/Services/db/postgresql
# 使用旧版 docker-compose 启动
```

## 回滚方案

如果升级失败，可以回滚：

```bash
# 停止新容器
docker-compose down

# 恢复旧数据
rm -rf ~/Services/db/postgresql17
cp -r ~/Services/db/postgresql-backup-xxx ~/Services/db/postgresql

# 使用旧版配置启动
# （需要保留旧的 docker-compose.yaml 和 Dockerfile）
```

## 参考文档

- [PostgreSQL 17 发布说明](https://www.postgresql.org/docs/17/release-17.html)
- [Alpine Linux 3.20 变更](https://alpinelinux.org/posts/Alpine-3.20.0-released.html)
- [Docker Compose 文件参考](https://docs.docker.com/compose/compose-file/)
