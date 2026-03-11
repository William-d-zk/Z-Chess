# Z-Chess Docker 升级指南

> 指导如何从旧版本升级到最新版本

---

## 📋 升级前准备

### 1. 备份数据
```bash
# 备份 PostgreSQL 数据
docker exec db-pg pg_dump -U chess -d isahl_9.x > backup_$(date +%Y%m%d).sql

# 备份 Raft 数据
cp -r ~/Z-Chess/raft00 ~/Z-Chess/backup/raft00_$(date +%Y%m%d)
cp -r ~/Z-Chess/raft01 ~/Z-Chess/backup/raft01_$(date +%Y%m%d)
cp -r ~/Z-Chess/raft02 ~/Z-Chess/backup/raft02_$(date +%Y%m%d)
```

### 2. 记录当前版本
```bash
# 记录当前镜像版本
docker images | grep z-chess

# 记录容器状态
docker ps --format "table {{.Names}}\t{{.Status}}"
```

---

## 🚀 标准升级流程

### 方式一: 平滑升级 (推荐)

```bash
# 1. 进入项目目录
cd Z-Chess/scripts

# 2. 拉取最新代码
git pull origin main

# 3. 停止旧容器 (保留数据)
docker compose -p z-chess-endpoint down

# 4. 更新镜像
docker compose -p z-chess-endpoint pull

# 5. 重新构建应用
cd ..
mvn clean package -pl Z-Arena -am -DskipTests

# 6. 启动新容器
cd scripts
docker compose -p z-chess-endpoint up -d --build

# 7. 验证升级
docker ps
docker logs raft00 | tail -20
```

### 方式二: 全新部署

```bash
# 1. 停止并删除旧容器
docker compose -p z-chess-endpoint down -v

# 2. 删除旧镜像
docker rmi z-chess-arena:2.0-arm64 z-chess-postgres:17.8-arm64

# 3. 重新部署
cd scripts
./docker-compose-aarch64.sh
```

---

## 📊 版本兼容性

| 版本 | 数据库 | JDK | 兼容性 |
|------|--------|-----|--------|
| 1.0.19 | PostgreSQL 17 | 17 | ✅ 完全兼容 |
| 1.0.18 | PostgreSQL 16 | 17 | ⚠️ 需要迁移 |
| 1.0.17 | PostgreSQL 16 | 11 | ❌ 不兼容 |

---

## 🔧 常见问题

### 升级后容器无法启动

**现象**: `docker ps` 显示容器 Exit

**解决**:
```bash
# 查看错误日志
docker logs raft00

# 常见问题:
# 1. 端口冲突 - 修改 docker-compose 端口映射
# 2. 权限问题 - sudo chown -R 999:999 ~/Z-Chess
# 3. 配置错误 - 检查 application.properties
```

### 数据库连接失败

**现象**: 日志显示 `Connection refused`

**解决**:
```bash
# 检查数据库容器
docker exec db-pg pg_isready

# 如果失败，重启数据库
docker restart db-pg

# 检查网络
docker network inspect z-chess-endpoint_endpoint
```

### Raft 节点无法加入集群

**现象**: 日志显示 `connection build failed`

**解决**:
```bash
# 1. 检查节点间连通性
docker exec raft00 ping raft01
docker exec raft00 ping raft02

# 2. 清理旧数据重新加入
rm -rf ~/Z-Chess/raft02/*
docker restart raft02
```

---

## 🔄 回滚操作

如果升级失败，执行回滚:

```bash
# 1. 停止新容器
docker compose -p z-chess-endpoint down

# 2. 恢复数据库
docker exec -i db-pg psql -U chess -d isahl_9.x < backup_YYYYMMDD.sql

# 3. 恢复 Raft 数据
cp -r ~/Z-Chess/backup/raft00_YYYYMMDD/* ~/Z-Chess/raft00/

# 4. 使用旧镜像启动
docker compose -p z-chess-endpoint up -d
```

---

## 📝 升级检查清单

- [ ] 已备份数据
- [ ] 已记录当前版本
- [ ] 已检查版本兼容性
- [ ] 已停止旧容器
- [ ] 已更新镜像/代码
- [ ] 已启动新容器
- [ ] 已验证功能正常
- [ ] 已清理旧镜像 (可选)

---

## 📞 获取帮助

遇到问题?

1. 查看日志: `docker logs <container>`
2. 查看文档: `CLUSTER_CONFIG_CHECK.md`
3. 提交 Issue: https://github.com/William-d-zk/Z-Chess/issues
