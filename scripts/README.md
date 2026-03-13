# Z-Chess Scripts 目录

本目录包含 Z-Chess 项目的容器化管理、构建和测试脚本。

## 特性

- **统一架构**: 使用官方 sqlite-jdbc，自动适配 AMD64/ARM64，无需区分架构
- **简化构建**: 单条命令构建，自动检测当前架构
- **容器化管理**: 完整的 Docker Compose 配置，支持 3 节点集群

## 目录结构

```
scripts/
├── archive/                    # 归档的过时脚本和旧架构配置
├── bin/                        # 容器管理脚本
│   ├── cleanup.sh             # 清理容器和资源
│   ├── health-check.sh        # 健康检查
│   ├── quick-start.sh         # 快速启动集群
│   ├── stop-cluster.sh        # 停止集群
│   └── view-logs.sh           # 查看日志
├── build/                      # 构建脚本
│   └── docker-build.sh        # 统一 Docker 构建脚本 (支持多架构)
├── deploy/                     # 部署配置
│   ├── kubernetes/            # Kubernetes 配置 (预留)
│   └── swarm/                 # Docker Swarm 配置 (预留)
├── docker/                     # Docker 配置
│   ├── Dockerfile             # 统一 Dockerfile (AMD64/ARM64 通用)
│   ├── docker-compose.yaml    # 3 节点集群配置
│   ├── postgres17/            # PostgreSQL 17 配置
│   └── test/                  # Docker 测试环境
│       ├── docker-compose.yaml
│       ├── docker-compose.mqtt-test.yaml
│       ├── Dockerfile.arena
│       ├── Dockerfile.audience
│       └── ...
├── test/                       # 测试脚本
│   ├── e2e/                   # 端到端测试
│   ├── integration/           # 集成测试
│   ├── tls/                   # TLS 测试
│   └── unit/                  # 单元测试
├── clean-and-restart.sh       # 清理并重启
├── init.sh                    # 初始化脚本
├── lib.sh                     # 通用库函数
└── README.md                  # 本文件
```

## 快速开始

### 1. 构建镜像

```bash
# 构建当前架构的镜像 (自动检测 AMD64/ARM64)
cd scripts/build
./docker-build.sh 1.0.22

# 或使用 buildx 构建多架构镜像
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f scripts/docker/Dockerfile \
  -t z-chess-arena:1.0.22 \
  .
```

### 2. 启动集群

```bash
# 方式1: 使用快速启动脚本
./bin/quick-start.sh

# 方式2: 使用 Docker Compose
cd docker
docker compose up -d

# 方式3: 使用测试环境
cd docker/test
docker compose -f docker-compose.mqtt-test.yaml up -d
```

### 3. 查看状态

```bash
# 健康检查
./bin/health-check.sh

# 查看日志
./bin/view-logs.sh

# 停止集群
./bin/stop-cluster.sh
```

## 文档

- [集群配置检查](CLUSTER_CONFIG_CHECK.md)
- [跨 IDC 网关设计](CROSS_IDC_GATEWAY_DESIGN.md)
- [Docker 升级指南](DOCKER_UPGRADE_GUIDE.md)
- [本地测试指南](LOCAL_TEST_GUIDE.md)
- [快速测试](QUICK_TEST.md)

## 架构说明

### 统一 Dockerfile

使用 `bellsoft/liberica-openjdk-alpine:17.0.18-10` 作为基础镜像，该镜像支持多架构：
- AMD64 (x86_64)
- ARM64 (aarch64)

官方 sqlite-jdbc 会自动加载对应架构的 native 库，无需手动管理。

### 端口映射

| 服务 | 容器内端口 | 主机端口 (raft00) | 主机端口 (raft01) | 主机端口 (raft02) |
|-----|-----------|------------------|------------------|------------------|
| HTTP API | 8080 | 8080 | 8081 | 8082 |
| MQTT TCP | 1883 | 1883 | 1884 | 1885 |
| MQTT TLS | 2883 | 2883 | 2884 | 2885 |
| Debug | 8000 | 8000 | 8001 | 8002 |

## 变更记录

### 2024-03-13 架构合并

- 合并 `aarch64/` 和 `amd64/` 到统一的 `docker/Dockerfile`
- 使用官方 sqlite-jdbc，自动适配不同架构
- 简化构建脚本为单一的 `docker-build.sh`
- 归档旧的架构相关脚本

### 2024-03-13 目录重组

- 将 `aarch64/` 和 `amd64/` 移动到 `docker/`
- 将测试脚本整合到 `test/` 和 `docker/test/`
- 将构建脚本移动到 `build/`
- 归档过时的 FreeBSD 和旧版脚本到 `archive/`
- 更新所有路径引用
