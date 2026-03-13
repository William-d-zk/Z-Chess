# Z-Chess Scripts 目录

本目录包含 Z-Chess 项目的容器化管理、构建和测试脚本。

## 目录结构

```
scripts/
├── archive/                    # 归档的过时脚本
├── bin/                        # 容器管理脚本
│   ├── cleanup.sh             # 清理容器和资源
│   ├── health-check.sh        # 健康检查
│   ├── quick-start.sh         # 快速启动集群
│   ├── stop-cluster.sh        # 停止集群
│   └── view-logs.sh           # 查看日志
├── build/                      # 构建脚本
│   ├── aarch64/               # ARM64 架构构建
│   ├── amd64/                 # AMD64 架构构建
│   ├── docker-build-aarch64.sh
│   └── docker-build-amd64.sh
├── deploy/                     # 部署配置
│   ├── kubernetes/            # Kubernetes 配置
│   └── swarm/                 # Docker Swarm 配置
├── docker/                     # Docker 配置和测试
│   ├── aarch64/               # ARM64 Docker 配置
│   ├── amd64/                 # AMD64 Docker 配置
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
│   └── README.md
├── clean-and-restart.sh       # 清理并重启
├── init.sh                    # 初始化脚本
├── lib.sh                     # 通用库函数
└── README.md                  # 本文件
```

## 快速开始

### 启动集群

```bash
# 使用快速启动脚本
./bin/quick-start.sh

# 或使用 Docker Compose
cd docker/test
docker compose -f docker-compose.mqtt-test.yaml up -d
```

### 构建镜像

```bash
# ARM64
./build/docker-build-aarch64.sh

# AMD64
./build/docker-build-amd64.sh
```

### 运行测试

```bash
# MQTT 测试
cd docker/test
./start-mqtt-test.sh

# TLS 测试
cd test/tls
./tls-verification.sh
```

## 文档

- [集群配置检查](CLUSTER_CONFIG_CHECK.md)
- [跨 IDC 网关设计](CROSS_IDC_GATEWAY_DESIGN.md)
- [Docker 升级指南](DOCKER_UPGRADE_GUIDE.md)
- [本地测试指南](LOCAL_TEST_GUIDE.md)
- [快速测试](QUICK_TEST.md)

## 变更记录

### 2024-03-13 目录重组

- 将 `aarch64/` 和 `amd64/` 移动到 `docker/`
- 将测试脚本整合到 `test/` 和 `docker/test/`
- 将构建脚本移动到 `build/`
- 归档过时的 FreeBSD 和旧版脚本到 `archive/`
- 更新所有路径引用
