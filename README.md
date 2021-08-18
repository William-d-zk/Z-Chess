# Z-Chess

* 以Chess为寓意，按照远近关系做了module的命名

## Z-King 事件处理组件

* 提供了以disruptor为基座的事件处理框架。
* 提供了基础的拓扑结构的ZUID设计，拥有复杂的分段结构，共64bit
* 提供了基于TimeWheel的大容量时间调度器，最小时钟分片为1秒钟，
* 为异步处理过程设计了进度【ZProgress】和返回包装结构【ZResponse】
* NTRU 加密算法组件
* 以及其他各种基础框架需要具备的基础设计。

## Z-Queen IO处理组件

* 提供了基于AIO的 socket 链式通讯处理机制，已支持TCP/TLS/ZLS，「UDP有待实现」
    * ZLS 目前仅支持了单边认证，使用 NTRU(RC4)的模式需要注意秘钥强度
    * ZLS 需要ZProtocol规范的交换协议予以支持，详见 @Z-Bishop
* 提供了基本协议抽象IProtocol，存在两层协议序列化标记 superSerial 和 Serial 分别指代当前协议的group类型和实体类型
* 提供了对象KV存储操作的抽象IDao，以及可被存储操作的抽象IStorage
* 提供了IO接口的Config体系
* Socket体系对服务与集群处理使用了隔离处理单元的设计，避免设计层的资源复用，仅在平台层进行复用。
* ServerCore:带有socket应答与集群能力的服务核心。详细的IO-Pipeline需要参考ServerCore中的说明
* ClusterCore:面向仅有集群间通信时使用，多用于servlet或springboot-rest微服务结构，非常便于融合springboot

## Z-Bishop 通讯协议

* 支持了MQTT 3.1.1 | 5.0.0，尚有大量协议规范的broker行为尚未支持。
* 完整支持了Websocket协议，并基于此设计了ZChat，用于服务通讯以及集群间通讯
* 尚未提供便于二次开发使用的协议处理链扩展点。已内置了websocket-json协议处理器。

## Z-Knight RAFT集群

* 集成了springboot-jpa作为应用数据存储
* 集成了ehcache作为中间层的cache，统一使用JPA进行管理。
* 集成了springboot-rest作为服务接口层
* 集群管理职能开发中。

## Z-Rook 数据存储

* 通过JPA结构进行数据库的DAO层，数据库默认为Postgresql
* 集成了Springboot-cache/ehcache 作为缓存层

## Z-Pawn endpoint，边缘服务；使用MQTT协议为嵌入性系统提供接入服务。

* 基于Knight的集群体系，同时支持单机部署和集群部署。
* 提供了接入设备以及消息存储管理结构，由于websocket协议并没有规范消息处理的结构所以依据MQTT的消息处理行为规范为蓝本设定了消息管理体系

## Z-Player 基于endpoint的 open api

## Z-Referee 安全策略、授权内控

## Z-Audience 测试用例等

## Z-Arena 网关

# 安装部署注意事项

## 数据库

> 需要手工创建数据库，使用 `PostgreSQL`
>
> 配置文件位置: `Z-Rook/src/main/resource/db.properties`

## DNS 配置

需要在集群中配置resolve.conf `search:isahl.com`
DB-Hostname:db-pg endpoint-Hostname:raft10,raft11,raft12
> 局域网内DNS需配置 `172.30.10.10 raft10;172.30.10.11 raft11;172.30.10.12 raft12`
从而通过DNS进行集群IP动态自适应，从而解决在云原生环境中，主机IP迁移引起的集群拓扑变更问题。

* spring.datasource.url=jdbc:postgresql://`db-pg.isahl.com`:5432/isahl.z-chess

> 需要在局域网内配置 db-pg.isahl.com 域名解析 DB hostname:db-pg search: isahl.com

## scripts

> update_version.sh 需要在 `${base.dir}` 中执行 
