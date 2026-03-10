# 检索集群处理算法 - 功能特性清单

> 文档生成时间: 2026-03-10  
> 基于 Z-Chess 项目代码分析

---

## 一、Raft 共识算法核心

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **Leader 选举** | 支持基于 Term 的领导者选举，包含 Pre-vote 机制避免无效选举 | `RaftPeer.startVote()` |
| **日志复制** | Leader 向 Followers 批量复制日志条目，支持 Pipeline 流水线优化 | `RaftPeer.followersAppend()` |
| **日志一致性** | 基于 Index@Term 的日志对齐和冲突检测，支持日志回滚 | `RaftPeer.catchUp()` / `rollback()` |
| **成员变更** | Joint Consensus 联合一致性算法，支持两阶段成员变更（COLD → JOINT → CNEW） | `RaftPeer.startMembershipChange()` |
| **Leader 转让** | 支持主动 Leadership Transfer，自动选择最佳目标节点 | `RaftPeer.transferLeadership()` |
| **Learner 节点** | 支持非投票的 Learner 角色，用于异步数据复制 | `LearnerManager` |

---

## 二、集群状态管理

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **状态机模型** | 8 种状态：OUTSIDE/CLIENT/FOLLOWER/ELECTOR/CANDIDATE/LEADER/GATE/LEARNER | `RaftState` |
| **拓扑管理** | RaftGraph 维护集群拓扑关系，支持双图（SelfGraph + JointGraph） | `RaftGraph` |
| **会话管理** | 基于 AIO 的集群会话管理，支持公平负载均衡 | `IManager.fairLoadSessionByPrefix()` |
| **心跳机制** | 可配置的心跳间隔和选举超时，支持 CheckQuorum 多数派检测 | `RaftPeer.heartbeat()` / `checkQuorum()` |
| **节点信息** | 包含 host/port/gate_host/gate_port 的完整节点描述 | `RaftNode` |

---

## 三、一致性服务

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **强一致性写** | 写操作需多数派确认后才返回成功 | `IConsistencyService.submit()` |
| **一致性读** | 支持 LeaseRead（租约读）和 ReadIndex 两种线性一致性读 | `RaftPeer.onReadIndex()` / `LeaseManager` |
| **提交确认** | GroupCommit 批量提交优化 | `GroupCommitManager` |
| **事务管理** | 成员变更事务状态机（PREPARING/JOINT/COMMITTING/CONFIRMED） | `MembershipChangeManager` |
| **一致性映射** | 坐标映射和状态追加 | `IConsistencyMapping` |

---

## 四、消息交换与路由

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **集群事件分发** | DecodedDispatcher 解码后分发到集群处理器或逻辑工作线程 | `DecodedDispatcher` |
| **消息交换服务** | IExchangeService 支持节点间消息转发 | `IExchangeService` |
| **路由注入** | IMappingCustom 支持自定义路由注入逻辑 | `IMappingCustom.inject()` |
| **一致性解包** | 将一致性消息解包为业务数据 | `IConsistencyBackload.unbox()` |
| **事件类型** | SINGLE/BATCH/SERVICE/IGNORE 等多种事件处理模式 | `OperateType` |

---

## 五、数据持久化与恢复

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **WAL 日志** | 基于 Raft 的 Write-Ahead-Log | `IRaftMapper.append()` |
| **快照机制** | 支持分片传输的大快照，自动触发和手动触发 | `RaftPeer.sendSnapshot()` / `takeSnapshot()` |
| **状态恢复** | 启动时从持久化状态恢复，支持成员变更状态恢复 | `RaftPeer.restoreMembershipChangeState()` |
| **日志压缩** | 基于快照的日志前缀截断 | `IRaftMapper.truncatePrefix()` |
| **元数据管理** | LogMeta/SnapshotMeta 管理日志和快照元信息 | `LogMeta` / `SnapshotMeta` |

---

## 六、集群通信协议

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **ZChat 协议** | 自定义集群通信协议，支持加密传输 | `ZClusterFactory` / `ZClusterCustom` |
| **命令类型** | Vote/Ballot/Append/Accept/Reject/Notify/Modify/Confirm/Snapshot 等 | `X70_RaftVote` ~ `X84_RaftLeaseReadResp` |
| **身份验证** | X08_Identity 节点身份交换 | `X08_Identity` |
| **多绑定支持** | 支持多端口绑定和网关模式 | `RaftNode.gate_host` / `gate_port` |
| **集群连接** | 自动建立集群节点间连接（peer < self 时主动连接） | `RaftPeer.graphUp()` |

---

## 七、检索与图遍历

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **图接口** | IGraph 支持节点创建、边连接、场景关联 | `IGraph<V>` |
| **搜索接口** | ISearcher 支持 BFS/DFS 遍历（预留接口） | `ISearcher<V>` / `BFS` / `DFS` |
| **节点查询** | 按类型和场景条件搜索节点 | `IGraph.search()` |
| **关联查询** | 支持视角关联和目标关联双向查询 | `IGraph.getPerspectiveAssociations()` |
| **图遍历** | 支持从指定节点进行遍历，支持扩散事件处理器 | `IPropagator` / `IVisitor` |

---

## 八、高可用与容错

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **故障检测** | 基于超时的故障检测，自动触发选举 | `ScheduleHandler` / `TimeWheel` |
| **网络分区** | CheckQuorum 机制防止脑裂 | `RaftPeer.checkQuorum()` |
| **优雅降级** | CLIENT 模式下可转发请求到 Leader | `RaftPeer.onSubmit()` |
| **超时回滚** | 成员变更和 Leadership 转让超时自动回滚 | `onMembershipChangeTimeout()` |
| **租约失效** | Leader step down 时自动失效读租约 | `LeaseManager.invalidateLease()` |

---

## 九、监控与治理

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **状态查询** | REST API 查询集群拓扑和 Raft 状态 | `ClusterController` |
| **配置变更** | 运行时动态修改集群配置 | `ConsistencyController` |
| **日志追踪** | 支持 origin→peer→client 的调用链追踪 | `ITraceable` |
| **健康检查** | Health 接口监控处理线程健康状态 | `Health` / `IHealth` |
| **节点管理** | 支持添加/删除/修改集群节点 | `RaftConfig.change()` |

---

## 十、性能优化

| 特性 | 描述 | 核心类/接口 |
|------|------|------------|
| **Pipeline 复制** | 支持 inflight 窗口的流水线日志复制 | `PipelineReplicationManager` |
| **批量处理** | 批量日志追加和批量通知 | `followersAppend()` 批量发送 |
| **异步 IO** | 基于 Java AIO 的高性能网络通信 | `AioSession` / `AioManager` |
| **Disruptor** | 基于 LMAX Disruptor 的高性能事件处理 | `RingBuffer<QEvent>` |
| **负载均衡** | 基于哈希的公平负载均衡算法 | `IManager.fairLoadSessionByPrefix()` |
| **批量同步** | 支持批量日志条目同步 | `_SyncBatchMaxSize` 配置 |

---

## 核心类关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                        集群架构总览                              │
└─────────────────────────────────────────────────────────────────┘

IClusterNode (集群节点接口)
    ├── buildConnector() / buildServer()  # 构建连接器和服务器
    └── RaftPeer (Raft 节点实现)
            ├── RaftMachine (状态机) ──► IRaftMachine
            ├── RaftGraph (拓扑图) ────► _SelfGraph / _JointGraph
            ├── IConsistencyService ───► 一致性服务接口
            └── IExchangeService ──────► 消息交换服务

IClusterCustom (集群事件处理接口)
    ├── onTimer()      # 心跳/选举超时处理
    ├── consistent()   # 强一致性处理
    ├── change()       # 拓扑变更处理
    └── waitForCommit() # 等待提交确认

IRaftService (Raft 服务接口)
    ├── getLeader()    # 获取 Leader 信息
    ├── topology()     # 获取集群拓扑
    ├── raftState()    # 获取 Raft 状态
    ├── startMembershipChange()  # 成员变更
    └── transferLeadership()     # Leader 转让

IConsistency (一致性接口)
    ├── IConsistent (序列化接口)
    └── ITraceable (可追踪接口)
        ├── origin()   # 消息来源
        └── client()   # 客户端标识
```

---

## Raft 消息类型清单

| 消息码 | 名称 | 说明 |
|--------|------|------|
| X6E | RaftPreVoteResp | Pre-vote 响应 |
| X6F | RaftPreVote | Pre-vote 请求 |
| X70 | RaftVote | 投票请求 |
| X71 | RaftBallot | 投票响应（选票） |
| X72 | RaftAppend | 日志追加请求 |
| X73 | RaftAccept | 日志接受确认 |
| X74 | RaftReject | 日志拒绝/冲突 |
| X75 | RaftReq | 客户端请求转发 |
| X76 | RaftResp | 响应结果 |
| X77 | RaftNotify | 日志提交通知 |
| X78 | RaftModify | 成员变更请求 |
| X79 | RaftConfirm | 变更确认 |
| X7A | RaftJoint | 联合一致状态 |
| X7B | RaftConfirm | 确认响应 |
| X7D | RaftSnapshot | 快照分片 |
| X7E | RaftSnapshotAck | 快照确认 |
| X7F | RaftReadIndex | 读索引请求 |
| X80 | RaftReadIndexResp | 读索引响应 |
| X81 | RaftTransferLeadership | Leader 转让请求 |
| X82 | RaftTransferLeadershipResp | Leader 转让响应 |
| X83 | RaftLeaseRead | 租约读请求 |
| X84 | RaftLeaseReadResp | 租约读响应 |

---

## 配置参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `electInSecond` | 选举超时时间 | 可配置 |
| `heartbeatInSecond` | 心跳间隔 | 可配置 |
| `snapshotFragmentMaxSize` | 快照分片最大大小 | 可配置 |
| `syncBatchMaxSize` | 批量同步最大条目数 | 可配置 |
| `snapshotMinSize` | 触发快照的最小日志大小 | 可配置 |
| `pipelineEnabled` | 是否启用 Pipeline 复制 | true |
| `leaseReadEnabled` | 是否启用租约读 | true |
| `learnerReplicationEnabled` | 是否启用 Learner 复制 | true |

---

*文档结束*
