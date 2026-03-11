# Z-Chess 测试脚本

本目录包含 Z-Chess 的各种测试脚本和工具。

## 目录结构

```
test/
├── docker/          # Docker 测试集群
│   ├── docker-compose.yaml    # Docker Compose 配置
│   ├── test-cluster.sh        # 集群管理脚本
│   ├── quick-start-test.sh    # 一键启动脚本
│   └── verify-audience.sh     # Audience 镜像验证
├── e2e/             # 端到端测试
│   ├── e2e-test.sh
│   ├── register-devices.sh
│   └── ...
└── tls/             # TLS 测试
    ├── generate-ssl-certs.sh  # 证书生成脚本
    └── README.md              # TLS 配置指南
```

## 快速开始

### Docker 测试集群

```bash
cd scripts/test/docker

# 一键启动
./quick-start-test.sh

# 或手动管理
./test-cluster.sh start    # 启动集群
./test-cluster.sh status   # 查看状态
./test-cluster.sh test     # 运行测试
./test-cluster.sh stop     # 停止集群
```

访问地址:
- Raft00: http://localhost:8080
- Raft01: http://localhost:8081
- Raft02: http://localhost:8082
- Audience: http://localhost:8090

### TLS 测试

```bash
cd scripts/test/tls

# 生成证书
./generate-ssl-certs.sh dev changeit

# 查看配置指南
cat README.md
```

### 端到端测试

```bash
cd scripts/test/e2e

# 运行 E2E 测试
./e2e-test.sh
```

## 详细文档

- [Docker 测试集群](../DOCKER_TEST_CLUSTER.md) - 完整使用指南
- [TLS 配置指南](../../TLS_SETUP_GUIDE.md) - TLS/SSL 配置说明
