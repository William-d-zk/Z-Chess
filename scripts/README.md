# Z-Chess 部署脚本合集

## 文档导航

| 文档 | 说明 |
|------|------|
| [QUICK_TEST.md](QUICK_TEST.md) | 5 分钟快速测试指南 |
| [LOCAL_TEST_GUIDE.md](LOCAL_TEST_GUIDE.md) | 完整的单机测试文档 |
| [DOCKER_UPGRADE_GUIDE.md](DOCKER_UPGRADE_GUIDE.md) | Docker 升级指南 (PostgreSQL 17) |
| [CLUSTER_CONFIG_CHECK.md](CLUSTER_CONFIG_CHECK.md) | 集群配置检查报告 |
| [CROSS_IDC_GATEWAY_DESIGN.md](CROSS_IDC_GATEWAY_DESIGN.md) | 跨 IDC Gateway 设计方案 |

## 快速命令

```bash
cd scripts

# 启动集群
./bin/quick-start.sh

# 健康检查
./bin/health-check.sh

# 查看日志
./bin/view-logs.sh

# 停止集群
./bin/stop-cluster.sh

# 清理环境
./bin/cleanup.sh
```

## 目录结构

```
scripts/
├── README.md                      # 本文件
├── QUICK_TEST.md                  # 快速测试指南
├── LOCAL_TEST_GUIDE.md            # 单机测试完整文档
├── DOCKER_UPGRADE_GUIDE.md        # Docker 升级指南
├── CLUSTER_CONFIG_CHECK.md        # 集群配置检查
├── CROSS_IDC_GATEWAY_DESIGN.md    # 跨 IDC 设计方案
├── bin/                           # 可执行脚本
│   ├── quick-start.sh            # 快速启动
│   ├── stop-cluster.sh           # 停止集群
│   ├── view-logs.sh              # 查看日志
│   ├── health-check.sh           # 健康检查
│   └── cleanup.sh                # 清理环境
├── aarch64/                       # ARM64 架构配置
│   ├── Dockerfile
│   └── Docker-Compose.yaml
├── amd64/                         # AMD64 架构配置
│   ├── Dockerfile
│   └── Docker-Compose.yaml
└── postgres17/                    # PostgreSQL 17 配置
    ├── Dockerfile
    ├── postgresql.conf
    └── init-scripts/
```

## 系统要求

- Docker 20.10+
- Docker Compose 2.0+
- 4 核 CPU / 8GB 内存（最低）
- 8 核 CPU / 16GB 内存（推荐）

## 端口占用

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | HTTP API | REST API 入口 |
| 8000 | Debug | Java 调试端口 |
| 1883 | MQTT | MQTT Broker |
| 1884-1885 | MQTT | 其他节点 MQTT |
| 5432 | PostgreSQL | 数据库 |
| 5228 | Cluster | ZChat 集群协议 |
| 5300 | Gateway | Gateway 服务 |

## 支持的平台

- [x] Linux AMD64
- [x] Linux ARM64
- [x] macOS (Docker Desktop)
- [x] Windows (WSL2 + Docker)

## 获取帮助

查看具体文档或运行脚本时添加 `-h` 参数。
