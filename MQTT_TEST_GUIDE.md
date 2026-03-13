# Z-Chess MQTT v3/v5 测试指南

## 快速开始

### 1. 启动测试环境

```bash
cd scripts/test/docker

# 前台启动 (查看实时日志)
./start-mqtt-test.sh up

# 后台启动
./start-mqtt-test.sh up -d

# 重新构建后启动
./start-mqtt-test.sh up --build
```

### 2. 服务访问

| 服务 | 地址 | 说明 |
|-----|------|------|
| raft00 (主节点) | http://localhost:8080 | HTTP API |
| | tcp://localhost:1883 | MQTT v3/v5 |
| | tcp://localhost:2883 | MQTT over TLS |
| raft01 | http://localhost:8081 | HTTP API |
| | tcp://localhost:1884 | MQTT v3/v5 |
| raft02 | http://localhost:8082 | HTTP API |
| | tcp://localhost:1885 | MQTT v3/v5 |
| Audience | http://localhost:8090 | 测试控制 API |

### 3. 查看日志

```bash
# 所有服务日志
./start-mqtt-test.sh logs

# 特定服务日志
docker logs -f raft00
docker logs -f audience
```

### 4. 停止环境

```bash
./start-mqtt-test.sh down
```

## MQTT 测试

### 使用 mosquitto 客户端测试

```bash
# 安装 mosquitto 客户端
# macOS: brew install mosquitto
# Ubuntu: apt-get install mosquitto-clients

# MQTT v3.1.1 连接测试
mosquitto_sub -h localhost -p 1883 -t "test/topic" -v

# MQTT v5 连接测试
mosquitto_sub -h localhost -p 1883 -t "test/topic" -v -V 5

# 发布消息
mosquitto_pub -h localhost -p 1883 -t "test/topic" -m "Hello MQTT"

# TLS 连接测试
mosquitto_sub -h localhost -p 2883 -t "test/topic" -v --cafile cert/ca.crt
```

### 使用 MQTT.fx/MQTTX 图形客户端

1. 新建连接
2. 配置 Broker: `localhost:1883`
3. 订阅主题: `test/#`
4. 发布测试消息

## 专项测试

### MQTT v3.1.1 专项测试

```bash
./start-mqtt-test.sh test-mqtt3
docker exec -it mqtt3-tester sh
```

### MQTT v5.0 专项测试

```bash
./start-mqtt-test.sh test-mqtt5
docker exec -it mqtt5-tester sh
```

## 健康检查

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# 检查集群状态
curl http://localhost:8080/api/cluster/status
```

## 故障排查

```bash
# 查看容器状态
docker-compose -f docker-compose.mqtt-test.yaml ps

# 进入容器调试
docker exec -it raft00 sh
docker exec -it audience sh

# 检查网络连通性
docker exec -it audience nc -zv raft00 1883

# 查看数据库
docker exec -it db-pg psql -U chess -d isahl_9.x
```

## 命令参考

```bash
./start-mqtt-test.sh [命令] [选项]

命令:
  up              启动测试环境
  down            停止测试环境
  restart         重启测试环境
  logs            查看日志
  test-mqtt3      启动 MQTT v3.1.1 专项测试
  test-mqtt5      启动 MQTT v5.0 专项测试
  build           重新构建镜像
  clean           清理所有数据

选项:
  -d, --detach    后台运行
  --build         启动前重新构建镜像
```
