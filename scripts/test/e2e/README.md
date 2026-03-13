# Z-Chess 端到端测试框架

## 目录结构

```
scripts/e2e/
├── e2e-test.sh          # 主测试脚本（完整流程）
├── run-quick-test.sh    # 快速功能测试
├── register-devices.sh  # 设备批量注册工具
├── lib/
│   ├── test-utils.sh    # 测试工具库
│   └── mqtt-test.sh     # MQTT 测试工具
├── config/
│   └── test-config.yaml # 测试配置
├── data/                # 测试数据（临时）
└── reports/             # 测试报告输出
```

## 快速开始

### 1. 完整端到端测试

```bash
cd scripts/e2e
./e2e-test.sh
```

### 2. 快速功能验证

```bash
# 集群已启动时
./run-quick-test.sh
```

### 3. 大规模设备注册

```bash
./register-devices.sh --count 10000 --batch-size 50
```

## 测试流程

```
┌─────────────────┐
│  1. 启动集群     │
│  quick-start.sh │
└────────┬────────┘
         ▼
┌─────────────────┐
│  2. 健康检查     │
│  health-check   │
└────────┬────────┘
         ▼
┌─────────────────┐
│  3. 启动客户端   │
│  test-client    │
└────────┬────────┘
         ▼
┌─────────────────┐
│  4. 批量注册     │
│  register-devices│
└────────┬────────┘
         ▼
┌─────────────────┐
│  5. 功能测试     │
│  functional tests│
└────────┬────────┘
         ▼
┌─────────────────┐
│  6. 压力测试     │
│  stress tests   │
└────────┬────────┘
         ▼
┌─────────────────┐
│  7. 收集结果     │
│  collect reports│
└────────┬────────┘
         ▼
┌─────────────────┐
│  8. 清理环境     │
│  cleanup        │
└─────────────────┘
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `TEST_DEVICE_COUNT` | 100 | 注册设备数量 |
| `TEST_CLIENT_COUNT` | 3 | 测试客户端数量 |
| `TEST_BATCH_SIZE` | 10 | 批次大小 |
| `RUN_STRESS_TEST` | false | 是否执行压力测试 |
| `STRESS_DURATION` | 10 | 压力测试持续时间(秒) |
| `SKIP_CLEANUP` | false | 跳过环境清理 |
| `STOP_CLUSTER_ON_EXIT` | true | 测试后停止集群 |

## 测试报告

测试完成后，报告位于 `reports/` 目录：

```
reports/
├── e2e-report-20250120_143022.json
├── e2e-test-20250120_143022.log
├── metrics-20250120_143022.json
└── logs-20250120_143022/
    ├── raft00.log
    ├── raft01.log
    └── raft02.log
```

## CI/CD 集成

```yaml
# GitHub Actions 示例
- name: E2E Test
  run: |
    cd scripts/e2e
    ./e2e-test.sh --device-count 50
  env:
    RUN_STRESS_TEST: true
    STRESS_DURATION: 30
```
