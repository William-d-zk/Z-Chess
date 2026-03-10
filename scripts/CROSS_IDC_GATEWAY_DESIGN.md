# Z-Chess 跨 IDC Gateway 互联设计方案

## 一、现有集群结构分析

### 1.1 当前架构

```
┌─────────────────────────────────────────────────────────────┐
│                    单 IDC 集群架构                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   raft00    │──│   raft01    │──│   raft02    │         │
│  │ 172.30.10.110│  │ 172.30.10.111│  │ 172.30.10.112│        │
│  │:5228(集群)   │  │:5228(集群)   │  │:5228(集群)   │        │
│  │:5300(Gate)  │  │              │  │              │        │
│  └──────┬──────┘  └─────────────┘  └─────────────┘         │
│         │                                                   │
│    ┌────┴────┐                                              │
│    │  Gate   │── 对外提供服务/转发                          │
│    │ Network │    172.30.11.0/24                            │
│    └─────────┘                                              │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 关键组件

| 组件 | 说明 | 配置项 |
|------|------|--------|
| `RaftNode` | 集群节点 | host:port + gateHost:gatePort |
| `RaftState.GATE` | 网关状态 | 标识节点为网关角色 |
| `endpoint` 网络 | 集群内部通信 | 172.30.10.0/24 |
| `gate` 网络 | 网关/外部通信 | 172.30.11.0/24 |
| `5228` 端口 | 集群协议端口 | ZChat 协议 |
| `5300` 端口 | 网关服务端口 | 对外提供 MQTT 等服务 |

### 1.3 现有 Gateway 配置

```properties
# raft.properties
z.chess.raft.config.gates[-4611686018427387904]=raft00/gate00:5300
z.chess.raft.config.gates[-4575657221408423936]=raft10/gate10:5300
z.chess.raft.config.gates[-4287426845256712192]=raft20/gate20:5300
```

**Gateway ID 计算规则**（基于 ZUID）：
- `C000#` = -4611686018427387904 (idc=0, cluster=0)
- `C080#` = -4575657221408423936 (idc=0, cluster=128)
- `C480#` = -4287426845256712192 (idc=0, cluster=576)

---

## 二、跨 IDC Gateway 互联设计

### 2.1 目标架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         跨 IDC 集群互联架构                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────┐      ┌─────────────────────────────┐  │
│  │       IDC-A (北京)          │      │       IDC-B (上海)          │  │
│  │  172.30.0.0/16              │◄────►│  172.31.0.0/16              │  │
│  ├─────────────────────────────┤      ├─────────────────────────────┤  │
│  │  ┌─────────┐ ┌─────────┐   │      │  ┌─────────┐ ┌─────────┐   │  │
│  │  │raft00-a │ │raft01-a │   │      │  │raft00-b │ │raft01-b │   │  │
│  │  │:5228    │ │:5228    │   │      │  │:5228    │ │:5228    │   │  │
│  │  └────┬────┘ └─────────┘   │      │  └────┬────┘ └─────────┘   │  │
│  │       │                    │      │       │                    │  │
│  │  ┌────┴────┐               │      │  ┌────┴────┐               │  │
│  │  │Gateway-A│◄──────────────┼──────┼──►│Gateway-B│               │  │
│  │  │:5300    │  专线/VPN     │      │  │:5300    │               │  │
│  │  │:5229    │  (跨IDC互联)   │      │  │:5229    │               │  │
│  │  └────┬────┘               │      │  └────┬────┘               │  │
│  │       │                    │      │       │                    │  │
│  │    [负载均衡]               │      │    [负载均衡]               │  │
│  └───────┼────────────────────┘      └───────┼────────────────────┘  │
│          │                                   │                         │
│          └──────────┬────────────────────────┘                         │
│                     │                                                  │
│              ┌──────┴──────┐                                           │
│              │  全局服务发现  │                                           │
│              │  (etcd/consul)│                                           │
│              └─────────────┘                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 互联模式设计

#### 模式一：Gateway-to-Gateway 直连（推荐）

```
┌────────────────────────────────────────────────────────────────┐
│                   Gateway 互联逻辑                              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   IDC-A Gateway                    IDC-B Gateway               │
│   ┌──────────────┐                 ┌──────────────┐           │
│   │  Z-Chess GW  │◄───────────────►│  Z-Chess GW  │           │
│   │  :5229       │  ZChat Cluster  │  :5229       │           │
│   └──────┬───────┘  Protocol       └──────┬───────┘           │
│          │                                 │                   │
│          ▼                                 ▼                   │
│   ┌──────────────┐                 ┌──────────────┐           │
│   │  Local Raft  │                 │  Local Raft  │           │
│   │  Cluster     │                 │  Cluster     │           │
│   └──────────────┘                 └──────────────┘           │
│                                                                │
│   特点：                                                       │
│   - 每个 IDC 有独立的 Raft 集群                                │
│   - Gateway 之间建立 ZChat 连接                                │
│   - 消息通过 Gateway 转发到目标 IDC                            │
│   - 支持 Topic 路由和设备定位                                  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

#### 模式二：联邦集群（Federation）

```
┌────────────────────────────────────────────────────────────────┐
│                  联邦集群互联逻辑                               │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│                     ┌─────────────┐                           │
│                     │  Meta Cluster│  (元数据集群)              │
│                     │  (3 nodes)   │  管理全局拓扑              │
│                     └──────┬──────┘                           │
│                            │                                   │
│            ┌───────────────┼───────────────┐                  │
│            │               │               │                  │
│     ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐         │
│     │  IDC-A      │ │  IDC-B      │ │  IDC-C      │         │
│     │  Sub-Cluster│ │  Sub-Cluster│ │  Sub-Cluster│         │
│     │  (3 nodes)  │ │  (3 nodes)  │ │  (3 nodes)  │         │
│     └─────────────┘ └─────────────┘ └─────────────┘         │
│                                                                │
│   特点：                                                       │
│   - 顶层 Meta Cluster 管理全局配置                             │
│   - 每个 IDC 是独立的 Sub-Cluster                              │
│   - 支持跨 IDC 的 Leader 选举                                  │
│   - 更复杂的架构，适合大规模多 IDC                              │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 三、详细设计方案

### 3.1 配置扩展

#### 3.1.1 新增 IDC 相关配置

```properties
# raft.properties - 跨 IDC 配置

# ==================== IDC 基础配置 ====================
z.chess.raft.config.uid.idc_id=0
z.chess.raft.config.uid.cluster_id=0

# ==================== 本地集群配置 ====================
z.chess.raft.config.peers[0]=raft00:5228
z.chess.raft.config.peers[1]=raft01:5228
z.chess.raft.config.peers[2]=raft02:5228

# ==================== 本地 Gateway ====================
z.chess.raft.config.gates[-4611686018427387904]=raft00/gate00:5300

# ==================== 跨 IDC Gateway 配置 ====================
# 格式: remote.gates[gateway_id]=gateway_host:gateway_port@idc_id
# IDC-1 (上海) 的 Gateway
z.chess.raft.config.remote.gates[-4575657221408423936]=sh-gateway.z-chess.com:5229@1
z.chess.raft.config.remote.gates[-4575657221408423937]=sh-gateway2.z-chess.com:5229@1

# IDC-2 (广州) 的 Gateway
z.chess.raft.config.remote.gates[-4287426845256712192]=gz-gateway.z-chess.com:5229@2

# ==================== 跨 IDC 路由策略 ====================
z.chess.raft.config.cross_idc.route_strategy=HASH_IDC_PREFERRED
# 可选策略:
# - SAME_IDC_FIRST: 优先同 IDC
# - HASH_IDC_PREFERRED: 哈希路由，优先命中 IDC
# - ROUND_ROBIN: 轮询所有 IDC
# - LEAST_LATENCY: 最低延迟优先

# ==================== 跨 IDC 心跳检测 ====================
z.chess.raft.config.cross_idc.heartbeat_interval=3S
z.chess.raft.config.cross_idc.heartbeat_timeout=9S
z.chess.raft.config.cross_idc.reconnect_backoff=1S,5S,10S

# ==================== 跨 IDC 安全 ====================
z.chess.raft.config.cross_idc.ssl.enabled=true
z.chess.raft.config.cross_idc.ssl.keystore=cross_idc/server.p12
z.chess.raft.config.cross_idc.ssl.truststore=cross_idc/trust.p12
```

#### 3.1.2 扩展 RaftNode 支持 IDC 信息

```java
public class RaftNode extends InnerProtocol {
    // 现有字段
    private String mHost;
    private int mPort;
    private String mGateHost;
    private int mGatePort;
    private int mState;
    
    // 新增字段
    private int mIdcId = 0;           // IDC 标识
    private int mClusterId = 0;       // 集群标识
    private String mRegion;           // 地域 (beijing/shanghai/guangzhou)
    private NodeType mType = NodeType.LOCAL;  // 节点类型
    
    public enum NodeType {
        LOCAL,      // 本地节点
        REMOTE,     // 远端 Gateway
        PROXY       // 代理节点
    }
}
```

### 3.2 Gateway 互联协议

```
┌────────────────────────────────────────────────────────────────┐
│              ZChat Protocol Extension (跨 IDC)                  │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  现有消息类型:                                                  │
│  - X70_RaftVote ~ X84_RaftLeaseReadResp                        │
│                                                                │
│  新增消息类型 (跨 IDC Gateway 之间):                            │
│                                                                │
│  ┌────────────────────────────────────────────────────────┐   │
│  │ X90_CrossIdcHandshake     - 跨 IDC 握手                 │   │
│  │ X91_CrossIdcTopologySync  - 拓扑同步                    │   │
│  │ X92_CrossIdcRouteQuery    - 路由查询                    │   │
│  │ X93_CrossIdcRouteResponse - 路由响应                    │   │
│  │ X94_CrossIdcForward       - 消息转发                    │   │
│  │ X95_CrossIdcForwardAck    - 转发确认                    │   │
│  │ X96_CrossIdcHealthCheck   - 健康检查                    │   │
│  │ X97_CrossIdcLatencyProbe  - 延迟探测                    │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

#### 消息定义示例

```java
/**
 * X94_CrossIdcForward - 跨 IDC 消息转发
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
public class X94_CrossIdcForward extends ZProtocol {
    private long targetPeerId;      // 目标节点 ID
    private int targetIdcId;        // 目标 IDC
    private byte[] payload;         // 转发的消息内容
    private long originPeerId;      // 源节点
    private int originIdcId;        // 源 IDC
    private long timestamp;         // 发送时间戳
    private ForwardType type;       // 转发类型
    
    public enum ForwardType {
        DEVICE_MESSAGE,     // 设备消息
        CLUSTER_CONSENSUS,  // 集群共识
        TOPIC_PUBLISH,      // Topic 发布
        DIRECT_RPC          // 直接 RPC
    }
}
```

### 3.3 路由策略实现

```java
/**
 * 跨 IDC 路由管理器
 */
@Component
public class CrossIdcRouteManager {
    
    // 本地 IDC ID
    @Value("${z.chess.raft.config.uid.idc_id:0}")
    private int localIdcId;
    
    // 所有 Gateway 连接
    private Map<Integer, List<GatewayConnection>> idcGateways = new ConcurrentHashMap<>();
    
    // 设备位置缓存 (deviceId -> idcId)
    private Cache<Long, Integer> deviceLocationCache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    /**
     * 路由消息到目标 IDC
     */
    public RouteResult route(Message message, RoutingStrategy strategy) {
        long targetId = message.getTargetDeviceId();
        
        // 1. 查询目标设备所在 IDC
        Integer targetIdc = deviceLocationCache.get(targetId, this::queryDeviceLocation);
        
        // 2. 如果目标在本地 IDC，直接处理
        if (targetIdc == localIdcId) {
            return RouteResult.local(message);
        }
        
        // 3. 根据策略选择 Gateway
        GatewayConnection gateway = selectGateway(targetIdc, strategy);
        
        // 4. 封装跨 IDC 转发消息
        X94_CrossIdcForward forward = new X94_CrossIdcForward();
        forward.setTargetPeerId(targetId);
        forward.setTargetIdcId(targetIdc);
        forward.setPayload(message.encode());
        
        return RouteResult.forward(gateway, forward);
    }
    
    /**
     * 选择 Gateway
     */
    private GatewayConnection selectGateway(int targetIdc, RoutingStrategy strategy) {
        List<GatewayConnection> gateways = idcGateways.get(targetIdc);
        
        return switch(strategy) {
            case LEAST_LATENCY -> gateways.stream()
                .min(Comparator.comparing(GatewayConnection::getLatency))
                .orElseThrow();
                
            case ROUND_ROBIN -> gateways.get(
                (int) (System.currentTimeMillis() % gateways.size()));
                
            case HASH -> gateways.get(
                Math.abs(targetIdc) % gateways.size());
                
            default -> gateways.get(0);  // 默认第一个
        };
    }
}
```

### 3.4 服务发现集成

```yaml
# application-cross-idc.yml

# 使用 etcd 作为跨 IDC 服务发现
spring:
  cloud:
    etcd:
      endpoints:
        - http://etcd-1.z-chess.com:2379
        - http://etcd-2.z-chess.com:2379
        - http://etcd-3.z-chess.com:2379
      
# 注册 Gateway 信息
z:
  chess:
    gateway:
      registration:
        enabled: true
        service-name: z-chess-gateway
        metadata:
          idc_id: ${z.chess.raft.config.uid.idc_id}
          cluster_id: ${z.chess.raft.config.uid.cluster_id}
          region: beijing
          public_address: gw-beijing.z-chess.com:5229
```

---

## 四、Docker Compose 多 IDC 部署示例

### 4.1 IDC-A (北京) 配置

```yaml
# docker-compose-idc-a.yml
version: '3.9'

services:
  # 本地 PostgreSQL
  db-pg:
    image: isahl/postgres:17.2-amd64
    container_name: db-pg
    networks:
      endpoint:
        ipv4_address: 172.30.10.254
    # ... 其他配置

  # Raft 节点
  raft00:
    image: img.z-chess.arena:2.0
    hostname: raft00
    networks:
      endpoint:
        ipv4_address: 172.30.10.110
      gate:
        ipv4_address: 172.30.11.110
    environment:
      - Z_CHESS_RAFT_UID_IDC_ID=0
      - Z_CHESS_RAFT_UID_CLUSTER_ID=0
      - Z_CHESS_CROSS_IDC_ENABLED=true
      - Z_CHESS_CROSS_IDC_GATEWAY_BIND=0.0.0.0:5229
    ports:
      - "8080:8080"
      - "1883:1883"
      - "5229:5229"  # 跨 IDC Gateway 端口

  # 跨 IDC Gateway 服务
  cross-idc-gateway:
    image: img.z-chess.cross-idc-gw:1.0
    hostname: cross-idc-gateway
    networks:
      gate:
        ipv4_address: 172.30.11.200
    environment:
      - LOCAL_IDC_ID=0
      - REMOTE_IDC_LIST=1,2
      - REMOTE_GATEWAY_1=sh-gateway.z-chess.com:5229
      - REMOTE_GATEWAY_2=gz-gateway.z-chess.com:5229
    ports:
      - "5229:5229"

networks:
  endpoint:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.10.0/24
  gate:
    driver: bridge
    ipam:
      config:
        - subnet: 172.30.11.0/24
```

### 4.2 IDC-B (上海) 配置

```yaml
# docker-compose-idc-b.yml
version: '3.9'

services:
  raft00:
    image: img.z-chess.arena:2.0
    hostname: raft00
    networks:
      endpoint:
        ipv4_address: 172.31.10.110
      gate:
        ipv4_address: 172.31.11.110
    environment:
      - Z_CHESS_RAFT_UID_IDC_ID=1
      - Z_CHESS_RAFT_UID_CLUSTER_ID=128
      - Z_CHESS_CROSS_IDC_ENABLED=true
      - Z_CHESS_CROSS_IDC_GATEWAY_BIND=0.0.0.0:5229
    ports:
      - "8080:8080"
      - "5229:5229"

networks:
  endpoint:
    driver: bridge
    ipam:
      config:
        - subnet: 172.31.10.0/24
  gate:
    driver: bridge
    ipam:
      config:
        - subnet: 172.31.11.0/24
```

### 4.3 网络互联配置

```bash
#!/bin/bash
# setup-cross-idc-network.sh

# 在 IDC-A 上执行
# 创建到 IDC-B 的 VPN/专线隧道

# 使用 WireGuard 示例
# 1. 生成密钥对
wg genkey | tee privatekey | wg pubkey > publickey

# 2. 配置 WireGuard 接口
cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
PrivateKey = $(cat privatekey)
Address = 10.255.0.1/24
ListenPort = 51820

# IDC-B
[Peer]
PublicKey = <IDC-B-PUBLIC-KEY>
AllowedIPs = 172.31.0.0/16, 10.255.0.2/32
Endpoint = sh-gateway.z-chess.com:51820
PersistentKeepalive = 25
EOF

# 3. 启动 WireGuard
wg-quick up wg0

# 4. 配置路由
ip route add 172.31.10.0/24 dev wg0
ip route add 172.31.11.0/24 dev wg0
```

---

## 五、关键实现代码

### 5.1 扩展 IClusterNode 支持跨 IDC

```java
public interface IClusterNode extends ILocalPublisher, IActivity {
    
    // 现有方法
    void setupPeer(String host, int port) throws IOException;
    void setupGate(String host, int port) throws IOException;
    
    // 新增跨 IDC 方法
    void setupCrossIdcGateway(String host, int port, int remoteIdcId) throws IOException;
    
    boolean isCrossIdcEnabled();
    
    CrossIdcGatewayManager getCrossIdcGatewayManager();
}
```

### 5.2 跨 IDC Gateway 管理器

```java
@Component
public class CrossIdcGatewayManager {
    
    private final Map<Integer, GatewayConnection> remoteGateways = new ConcurrentHashMap<>();
    
    @Autowired
    private IRaftConfig raftConfig;
    
    /**
     * 初始化连接到所有远端 Gateway
     */
    @PostConstruct
    public void init() {
        if (!raftConfig.isCrossIdcEnabled()) {
            return;
        }
        
        // 从配置加载远端 Gateway
        Map<Long, RaftNode> remoteGates = raftConfig.getRemoteGates();
        
        remoteGates.forEach((id, node) -> {
            connectToRemoteGateway(node);
        });
    }
    
    private void connectToRemoteGateway(RaftNode node) {
        try {
            IClusterNode clusterNode = getClusterNode();
            clusterNode.setupCrossIdcGateway(
                node.getHost(), 
                node.getPort(), 
                node.getIdcId()
            );
            
            // 发送握手消息
            sendHandshake(node);
            
        } catch (IOException e) {
            logger.error("Failed to connect to remote gateway: {}", node, e);
            scheduleReconnect(node);
        }
    }
}
```

---

## 六、部署检查清单

- [ ] 各 IDC 网络互通 (VPN/专线配置)
- [ ] 防火墙开放端口 (5228, 5229, 5300)
- [ ] DNS 解析配置 (跨 IDC hostname 可解析)
- [ ] SSL 证书配置 (跨 IDC 通信加密)
- [ ] 时区同步 (所有节点 NTP 同步)
- [ ] IDC ID 唯一性检查
- [ ] Gateway ID 正确计算 (ZUID)
- [ ] 服务发现 (etcd/consul) 可访问
- [ ] 监控告警配置 (跨 IDC 延迟监控)

---

## 七、性能考虑

| 指标 | 建议值 | 说明 |
|------|--------|------|
| 跨 IDC 延迟 | < 50ms | 同一国家内 |
| 跨 IDC 带宽 | > 100Mbps | 根据设备数量调整 |
| Gateway 连接数 | < 1000 | 每个 Gateway |
| 消息转发延迟 | < 10ms | 同 IDC 内 |
| 跨 IDC 消息延迟 | < 100ms | 含网络延迟 |

---

*文档版本: 1.0*
*适用于: Z-Chess 2.0+ 跨 IDC 部署*
