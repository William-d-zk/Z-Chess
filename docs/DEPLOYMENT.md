# Z-Chess 部署说明

## 1. 环境要求

### 1.1 系统要求

| 组件 | 最低要求 | 推荐配置 |
|-----|---------|---------|
| JDK | 17 LTS | 17/21 LTS |
| 内存 | 4GB | 8GB+ |
| CPU | 4核 | 8核+ |
| 磁盘 | 50GB SSD | 200GB+ NVMe |
| OS | Linux 4.x+ | Linux 5.x+ |

### 1.2 软件依赖

```bash
# 必需组件
- PostgreSQL 14+
- Docker 24+ (容器化部署)
- Maven 3.9+ (源码构建)

# 可选组件
- WolfSSL (高性能 TLS)
- OpenSSL 3.0+ (备用 TLS)
```

## 2. 源码构建

### 2.1 克隆代码

```bash
git clone https://github.com/William-d-zk/Z-Chess.git
cd Z-Chess
```

### 2.2 编译打包

```bash
# 完整构建 (跳过测试)
mvn clean package -DskipTests

# 构建特定模块
mvn clean package -pl Z-Arena -am -DskipTests

# 生产环境构建
mvn clean package -P prod -DskipTests
```

### 2.3 构建输出

```
${user.home}/Z-Chess/
├── Z-Arena/target/gateway.z-arena-1.0.20.jar      # 网关服务
├── Z-Audience/target/test.z-audience-1.0.20.jar   # 测试服务
└── ... 其他模块
```

## 3. 单机部署

### 3.1 数据库初始化

```bash
# 连接 PostgreSQL
psql -U postgres

# 创建数据库
CREATE DATABASE z_chess;
CREATE USER z_chess WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE z_chess TO z_chess;

# 退出
\q
```

### 3.2 配置文件准备

```bash
mkdir -p ~/z-chess/config ~/z-chess/logs ~/z-chess/data

# 复制配置模板
cp Z-Pawn/src/main/resources/application.properties ~/z-chess/config/
cp Z-Pawn/src/main/resources/db.properties ~/z-chess/config/

# 编辑配置
vim ~/z-chess/config/application.properties
```

### 3.3 启动服务

```bash
# 方式1: 直接启动
java -jar \
  -Dspring.config.location=~/z-chess/config/ \
  -Dlogging.file.name=~/z-chess/logs/z-chess.log \
  Z-Arena/target/gateway.z-arena-1.0.20.jar

# 方式2: 后台启动
nohup java -Xms2g -Xmx2g \
  -Dspring.config.location=~/z-chess/config/ \
  -jar Z-Arena/target/gateway.z-arena-1.0.20.jar \
  > ~/z-chess/logs/nohup.out 2>&1 &
```

### 3.4 JVM 参数优化

```bash
# 生产环境 JVM 参数
JAVA_OPTS="
  -server
  -Xms4g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+AlwaysPreTouch
  -XX:+DisableExplicitGC
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=~/z-chess/logs/
  -XX:+CrashOnOutOfMemoryError
  -Djava.security.egd=file:/dev/./urandom
  -Dfile.encoding=UTF-8
"

java $JAVA_OPTS -jar gateway.z-arena-1.0.20.jar
```

## 4. 集群部署 (3 节点 RAFT)

### 4.1 网络拓扑

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
         ┌────▼────┐  ┌────▼────┐  ┌────▼────┐
         │ Node 1  │  │ Node 2  │  │ Node 3  │
         │:2888/8080│  │:2888/8080│  │:2888/8080│
         │  LEADER │  │ FOLLOWER│  │ FOLLOWER│
         └────┬────┘  └────┬────┘  └────┬────┘
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────▼──────┐
                    │ PostgreSQL  │
                    └─────────────┘
```

### 4.2 节点配置

#### Node 1 (172.30.10.10)

```properties
# ~/z-chess/config/application-node1.properties
server.port=8080
raft.node.id=node1
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888
raft.port=2888
spring.datasource.url=jdbc:postgresql://db-pg.isahl.com:5432/z_chess
```

#### Node 2 (172.30.10.11)

```properties
# ~/z-chess/config/application-node2.properties
server.port=8080
raft.node.id=node2
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888
raft.port=2888
spring.datasource.url=jdbc:postgresql://db-pg.isahl.com:5432/z_chess
```

#### Node 3 (172.30.10.12)

```properties
# ~/z-chess/config/application-node3.properties
server.port=8080
raft.node.id=node3
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888
raft.port=2888
spring.datasource.url=jdbc:postgresql://db-pg.isahl.com:5432/z_chess
```

### 4.3 启动集群

```bash
# 在每个节点执行

# Node 1
nohup java -Xms2g -Xmx2g \
  -Dspring.profiles.active=node1 \
  -Dspring.config.location=~/z-chess/config/ \
  -jar gateway.z-arena-1.0.20.jar > ~/z-chess/logs/node1.log 2>&1 &

# Node 2
nohup java -Xms2g -Xmx2g \
  -Dspring.profiles.active=node2 \
  -Dspring.config.location=~/z-chess/config/ \
  -jar gateway.z-arena-1.0.20.jar > ~/z-chess/logs/node2.log 2>&1 &

# Node 3
nohup java -Xms2g -Xmx2g \
  -Dspring.profiles.active=node3 \
  -Dspring.config.location=~/z-chess/config/ \
  -jar gateway.z-arena-1.0.20.jar > ~/z-chess/logs/node3.log 2>&1 &
```

### 4.4 验证集群状态

```bash
# 检查节点状态
curl http://172.30.10.10:8080/api/cluster/status
curl http://172.30.10.11:8080/api/cluster/status
curl http://172.30.10.12:8080/api/cluster/status

# 预期响应
{
  "nodeId": "node1",
  "state": "LEADER",
  "term": 5,
  "peers": ["node2", "node3"]
}
```

## 5. Docker 部署

### 5.1 构建镜像

```bash
# 使用提供的脚本
chmod +x scripts/*.sh
./scripts/docker-build-amd64.sh

# 或手动构建
docker build -t z-chess:1.0.20 -f Dockerfile .
```

### 5.2 单机 Docker 运行

```bash
docker run -d \
  --name z-chess \
  --restart always \
  -p 8080:8080 \
  -p 1883:1883 \
  -p 2883:2883 \
  -e DB_PASSWORD=your_password \
  -v ~/z-chess/config:/app/config \
  -v ~/z-chess/logs:/app/logs \
  -v ~/z-chess/data:/app/data \
  z-chess:1.0.20
```

### 5.3 Docker Compose 集群部署

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: z_chess
      POSTGRES_USER: z_chess
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - z-chess-net

  node1:
    image: z-chess:1.0.20
    environment:
      - RAFT_NODE_ID=node1
      - RAFT_CLUSTER_NODES=node1:2888,node2:2888,node3:2888
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/z_chess
    ports:
      - "8080:8080"
      - "1883:1883"
    volumes:
      - ./config/node1:/app/config
    networks:
      - z-chess-net
    depends_on:
      - postgres

  node2:
    image: z-chess:1.0.20
    environment:
      - RAFT_NODE_ID=node2
      - RAFT_CLUSTER_NODES=node1:2888,node2:2888,node3:2888
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/z_chess
    ports:
      - "8081:8080"
    volumes:
      - ./config/node2:/app/config
    networks:
      - z-chess-net
    depends_on:
      - postgres

  node3:
    image: z-chess:1.0.20
    environment:
      - RAFT_NODE_ID=node3
      - RAFT_CLUSTER_NODES=node1:2888,node2:2888,node3:2888
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/z_chess
    ports:
      - "8082:8080"
    volumes:
      - ./config/node3:/app/config
    networks:
      - z-chess-net
    depends_on:
      - postgres

volumes:
  postgres_data:

networks:
  z-chess-net:
    driver: bridge
```

启动：

```bash
docker-compose up -d
```

## 6. Kubernetes 部署

### 6.1 ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: z-chess-config
data:
  application.properties: |
    server.port=8080
    spring.datasource.url=jdbc:postgresql://postgres:5432/z_chess
    mqtt.server.port=1883
---
```

### 6.2 StatefulSet (3 节点)

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: z-chess
spec:
  serviceName: z-chess-headless
  replicas: 3
  selector:
    matchLabels:
      app: z-chess
  template:
    metadata:
      labels:
        app: z-chess
    spec:
      containers:
      - name: z-chess
        image: z-chess:1.0.20
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 1883
          name: mqtt
        - containerPort: 2888
          name: raft
        env:
        - name: RAFT_NODE_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: RAFT_CLUSTER_NODES
          value: "z-chess-0:2888,z-chess-1:2888,z-chess-2:2888"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: z-chess-secret
              key: db-password
        volumeMounts:
        - name: config
          mountPath: /app/config
        - name: data
          mountPath: /app/data
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
      volumes:
      - name: config
        configMap:
          name: z-chess-config
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
```

部署：

```bash
kubectl apply -f z-chess-k8s.yaml
```

## 7. 证书配置

### 7.1 自签名证书生成

```bash
mkdir -p ~/z-chess/cert

# 生成 CA
openssl req -x509 -sha256 -days 3650 -newkey rsa:4096 \
  -keyout ~/z-chess/cert/ca.key -out ~/z-chess/cert/ca.crt \
  -subj "/C=CN/O=Z-Chess/CN=Z-Chess CA"

# 生成服务端证书
openssl req -new -newkey rsa:2048 -keyout ~/z-chess/cert/server.key -out ~/z-chess/cert/server.csr \
  -subj "/C=CN/O=Z-Chess/CN=*.isahl.com"

openssl x509 -req -sha256 -days 365 -in ~/z-chess/cert/server.csr \
  -CA ~/z-chess/cert/ca.crt -CAkey ~/z-chess/cert/ca.key \
  -out ~/z-chess/cert/server.crt -CAcreateserial

# 转换为 PKCS12
openssl pkcs12 -export -inkey ~/z-chess/cert/server.key \
  -in ~/z-chess/cert/server.crt -certfile ~/z-chess/cert/ca.crt \
  -out ~/z-chess/cert/server.p12 -name server
```

### 7.2 配置证书

```properties
server.ssl.enabled=true
server.ssl.key-store=~/z-chess/cert/server.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=server
```

## 8. 健康检查与监控

### 8.1 健康检查端点

```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# 数据库连接状态
curl http://localhost:8080/actuator/health/db

# 磁盘空间
curl http://localhost:8080/actuator/health/diskSpace
```

### 8.2 指标监控

```bash
# Prometheus 格式指标
curl http://localhost:8080/actuator/prometheus

# 应用信息
curl http://localhost:8080/actuator/info
```

## 9. 故障排查

### 9.1 查看日志

```bash
# 实时日志
tail -f ~/z-chess/logs/z-chess.log

# 错误日志
grep -E "ERROR|WARN" ~/z-chess/logs/z-chess.log

# 日志级别动态调整
curl -X POST http://localhost:8080/actuator/loggers/com.isahl.chess \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

### 9.2 常见问题

| 问题 | 原因 | 解决 |
|-----|------|------|
| 端口被占用 | 端口冲突 | 修改配置或停止占用进程 |
| 数据库连接失败 | 配置错误或网络问题 | 检查 db.properties |
| 集群无法选举 | 节点间网络不通 | 检查防火墙和端口 2888/2889 |
| 内存溢出 | JVM 参数不当 | 增大堆内存或优化 GC |

### 9.3 进程管理

```bash
# 查找进程
jps -lvm | grep z-arena

# 优雅停止
kill -15 <pid>

# 强制停止
kill -9 <pid>
```

## 10. 升级维护

### 10.1 滚动升级 (集群)

```bash
# 1. 升级 Follower 节点
curl -X POST http://node2:8080/api/cluster/drain
curl -X POST http://node3:8080/api/cluster/drain

# 2. 停止、升级、重启节点
# ... 对每个节点执行

# 3. 验证集群健康
curl http://node1:8080/api/cluster/status
```

### 10.2 数据备份

```bash
# 数据库备份
pg_dump -U z-chess z_chess > z_chess_backup_$(date +%Y%m%d).sql

# Raft 数据备份
tar -czf raft_backup_$(date +%Y%m%d).tar.gz ~/z-chess/raft/
```

## 11. 安全配置清单

- [ ] 修改默认密码
- [ ] 启用 TLS 1.3
- [ ] 配置防火墙 (仅开放必要端口)
- [ ] 数据库访问控制
- [ ] 日志审计启用
- [ ] JVM 安全参数配置

## 12. 生产环境检查清单

- [ ] JVM 参数优化
- [ ] 数据库连接池配置
- [ ] 日志轮转配置
- [ ] 监控告警配置
- [ ] 备份策略确认
- [ ] 故障转移测试
- [ ] 压力测试通过
