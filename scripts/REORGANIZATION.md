# 脚本目录重组说明

## 变更概要

将测试脚本从架构相关目录（`docker/aarch64/`）移动到功能相关目录（`scripts/test/`），使目录结构更清晰合理。

## 旧结构

```
scripts/
├── aarch64/                    # 混合了架构和测试
│   ├── Dockerfile              # 构建文件
│   ├── Docker-Compose-Test.yaml # 测试配置
│   ├── test-cluster.sh         # 测试脚本
│   ├── quick-start-test.sh     # 测试脚本
│   └── verify-audience.sh      # 测试脚本
├── amd64/                      # 只有构建文件
│   └── Dockerfile
├── e2e/                        # 端到端测试
└── bin/                        # 基础脚本
```

## 新结构

```
scripts/
├── test.sh                     # 统一测试入口
├── test/
│   ├── README.md               # 测试文档
│   ├── docker/                 # Docker 测试集群
│   │   ├── docker-compose.yaml
│   │   ├── test-cluster.sh
│   │   ├── quick-start-test.sh
│   │   └── verify-audience.sh
│   ├── e2e/                    # 端到端测试
│   └── tls/                    # TLS 测试
│       ├── generate-ssl-certs.sh
│       └── README.md
├── build/                      # 构建相关
│   ├── docker-build-aarch64.sh
│   ├── docker-build-amd64.sh
│   ├── aarch64/
│   │   ├── Dockerfile
│   │   └── Dockerfile.audience
│   └── amd64/
│       └── Dockerfile
├── bin/                        # 基础管理脚本
├── deploy/                     # 部署配置
└── aarch64/                    # 向后兼容入口
    └── test-cluster.sh (重定向)
```

## 变更详情

### 移动的文件

| 原位置 | 新位置 | 说明 |
|--------|--------|------|
| `docker/aarch64/Docker-Compose-Test.yaml` | `scripts/test/docker/docker-compose.yaml` | Docker 测试配置 |
| `docker/aarch64/test-cluster.sh` | `scripts/test/docker/test-cluster.sh` | 集群管理脚本 |
| `docker/aarch64/quick-start-test.sh` | `scripts/test/docker/quick-start-test.sh` | 快速启动脚本 |
| `docker/aarch64/verify-audience.sh` | `scripts/test/docker/verify-audience.sh` | 镜像验证脚本 |
| `docker/aarch64/Dockerfile.audience` | `build/aarch64/Dockerfile.audience` | Audience 镜像构建 |
| `scripts/generate-ssl-certs.sh` | `scripts/test/tls/generate-ssl-certs.sh` | 证书生成脚本 |
| `scripts/tls-verification.sh` | `scripts/test/tls/tls-verification.sh` | TLS 验证脚本 |

### 新增文件

| 文件 | 说明 |
|------|------|
| `scripts/test.sh` | 统一测试入口脚本 |
| `scripts/test/README.md` | 测试脚本说明文档 |
| `scripts/build/docker-build-aarch64.sh` | ARM64 构建脚本 |
| `scripts/build/docker-build-amd64.sh` | AMD64 构建脚本 |

### 更新的路径引用

- `docker-compose.yaml`: 更新 `dockerfile` 路径从 `docker/aarch64/` 到 `build/aarch64/`
- `quick-start-test.sh`: 更新 Maven 构建路径
- `DOCKER_TEST_CLUSTER.md`: 更新所有路径引用

### 向后兼容

旧位置的脚本现在会显示重定向提示并自动转发到新位置：

```bash
$ docker/aarch64/test-cluster.sh start
========================================
提示: 此脚本已移动到新的位置
========================================
旧位置: docker/aarch64/test-cluster.sh
新位置: scripts/test/docker/test-cluster.sh
...
```

## 使用方式

### 推荐方式（新）

```bash
# 统一入口
./scripts/test.sh docker start      # Docker 测试集群
./scripts/test.sh e2e               # 端到端测试
./scripts/test.sh tls               # TLS 测试

# 或直接访问
cd scripts/test/docker
./test-cluster.sh start
```

### 向后兼容（旧）

```bash
# 仍然可用，但会显示重定向提示
cd scripts/aarch64
./test-cluster.sh start
```

## 好处

1. **职责分离**: 架构相关文件在 `build/`，测试相关文件在 `test/`
2. **更清晰**: 按功能组织而非按架构组织
3. **易于发现**: 测试脚本集中在 `test/` 目录
4. **向后兼容**: 旧脚本仍然可用，逐步迁移
