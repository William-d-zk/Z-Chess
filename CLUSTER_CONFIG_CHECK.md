# Z-Chess 集群配置检查清单

> 本清单用于部署前检查集群配置是否正确

---

## 🔍 前置检查

### 系统要求
- [ ] Docker Engine >= 20.10
- [ ] Docker Compose >= 2.0
- [ ] 内存 >= 8GB
- [ ] 磁盘空间 >= 10GB

### 网络检查
- [ ] 端口 5432 (PostgreSQL) 未被占用
- [ ] 端口 8080-8082 (HTTP API) 未被占用
- [ ] 端口 1883-1885 (MQTT) 未被占用
- [ ] 端口 8000, 8100, 8200 (Debug) 未被占用

---

## 📋 配置文件检查

### 数据库配置
```bash
# 检查 db.properties
cat Z-Rook/src/main/resources/db.properties
```

- [ ] PostgreSQL 主机名正确
- [ ] PostgreSQL 端口正确 (默认 5432)
- [ ] 数据库名称正确 (默认 isahl_9.x)
- [ ] 用户名/密码正确

### Raft 集群配置
```bash
# 检查 raft.properties
cat Z-Knight/src/main/resources/raft.properties
```

- [ ] elect_in_second 已配置 (推荐 1S)
- [ ] heartbeat_in_second 已配置 (推荐 3S)
- [ ] peers 列表包含所有节点
- [ ] base_dir 目录可写入

### 网络配置
- [ ] 节点间可以互相访问端口 5228
- [ ] 节点可以访问数据库端口 5432
- [ ] Docker 网络配置正确

---

## 🚀 部署检查

### 数据目录
```bash
ls -la ~/Z-Chess/
```

- [ ] raft00/ 目录存在且可写
- [ ] raft01/ 目录存在且可写
- [ ] raft02/ 目录存在且可写
- [ ] logs/ 目录存在且可写

### 启动检查
```bash
docker compose -p z-chess-endpoint -f aarch64/Docker-Compose.yaml up -d
```

- [ ] 所有容器状态为 Up
- [ ] 数据库容器状态为 healthy
- [ ] 无端口冲突错误

---

## ✅ 功能验证

### API 测试
```bash
# 测试 HTTP API
curl http://localhost:8080/echo/hook?input=test

# 预期返回 JSON 包含 "成功"
```

- [ ] raft00 API 响应正常
- [ ] raft01 API 响应正常
- [ ] raft02 API 响应正常

### 数据库测试
```bash
docker exec -it db-pg psql -U chess -d isahl_9.x -c "SELECT 1;"
```

- [ ] 数据库连接正常
- [ ] 可以执行查询

### Raft 集群测试
```bash
# 查看日志确认 Leader 选举
docker logs raft00 | grep -E "LEADER|FOLLOWER"
```

- [ ] Leader 已选举
- [ ] 所有节点状态正常
- [ ] 日志复制正常

---

## 🔧 故障排查

### 容器无法启动
```bash
# 查看容器日志
docker logs <container_name>

# 检查资源使用
docker stats --no-stream
```

### 连接被拒绝
```bash
# 检查端口占用
lsof -i :<port>

# 检查防火墙
iptables -L | grep <port>
```

### 数据库连接失败
```bash
# 检查 PostgreSQL 容器
docker exec -it db-pg pg_isready

# 检查网络连通性
docker exec raft00 ping db-pg
```

---

## 📞 联系支持

如遇到无法解决的问题，请联系：
- 邮箱: william@isahl.com
- 项目: https://github.com/William-d-zk/Z-Chess
