# Z-Chess 代码库分析摘要

## 项目概述
- **总文件数**: 729
- **总代码行数**: 98462
- **包数量**: 216

## 模块结构

### com.isahl.chess.arena.gateway.rest
- 文件数: 1
- 主要类: RookCacheApi

### com.isahl.chess.arena.gateway.service
- 文件数: 1
- 主要类: RookCacheService

### com.isahl.chess.arena.start
- 文件数: 1
- 主要类: ApplicationArena

### com.isahl.chess.audience.arena
- 文件数: 1
- 主要类: ApplicationArenaTest

### com.isahl.chess.audience.bishop.mqtt.v5
- 文件数: 3
- 主要类: LastWillHandlerTest, Qos2HandlerTest, RetainedMessageStoreTest

### com.isahl.chess.audience.bishop.protocol.modbus
- 文件数: 3
- 主要类: ModbusTcpProtocolHandlerTest, ModbusTcpCodecTest, ModbusIntegrationTest

### com.isahl.chess.audience.bishop.protocol.modbus.exception
- 文件数: 1
- 主要类: ModbusExceptionTest

### com.isahl.chess.audience.bishop.protocol.modbus.function
- 文件数: 1
- 主要类: ModbusExtendedFunctionsTest

### com.isahl.chess.audience.bishop.protocol.modbus.master
- 文件数: 2
- 主要类: ModbusMasterTest, ModbusRequestResponseTest

### com.isahl.chess.audience.bishop.protocol.modbus.metrics
- 文件数: 1
- 主要类: ModbusMetricsTest

### com.isahl.chess.audience.bishop.protocol.modbus.model
- 文件数: 1
- 主要类: ModbusExceptionCodeTest

### com.isahl.chess.audience.bishop.protocol.modbus.rtu
- 文件数: 1
- 主要类: ModbusRtuCodecTest

### com.isahl.chess.audience.bishop.protocol.modbus.serial
- 文件数: 1
- 主要类: SerialPortConfigTest

### com.isahl.chess.audience.bishop.protocol.modbus.slave
- 文件数: 2
- 主要类: ModbusSlaveIntegrationTest, ModbusSlaveTest

### com.isahl.chess.audience.bishop.protocol.modbus.spi
- 文件数: 1
- 主要类: ModbusRtuProtocolHandlerTest

### com.isahl.chess.audience.bishop.protocol.modbus.tag
- 文件数: 3
- 主要类: DataTypeTest, DataTypeConverterTest, TagManagerTest

### com.isahl.chess.audience.bishop.protocol.modbus.tcp
- 文件数: 1
- 主要类: ModbusTlsConfigTest

### com.isahl.chess.audience.bishop.protocol.mqtt
- 文件数: 1
- 主要类: PasswordAuthProviderTest

### com.isahl.chess.audience.bishop.protocol.spi
- 文件数: 1
- 主要类: ProtocolLoaderTest

### com.isahl.chess.audience.client.api
- 文件数: 1
- 主要类: ConsumerController

### com.isahl.chess.audience.client.component
- 文件数: 2
- 主要类: ClientHandler, ClientPool

### com.isahl.chess.audience.client.config
- 文件数: 4
- 主要类: IoConsumerConfig, AudienceCacheConfig, ClientConfig, Target, SocketConfig

### com.isahl.chess.audience.client.model
- 文件数: 1
- 主要类: Client

### com.isahl.chess.audience.client.stress
- 文件数: 10
- 主要类: PressureTestValidator, StressClient, ApiTestController, StressApiRequest, FullTestRequest

### com.isahl.chess.audience.king.metrics
- 文件数: 1
- 主要类: ZChessMetricsTest

### com.isahl.chess.audience.knight
- 文件数: 2
- 主要类: SchedulingRuleTest, TestSchedulingRule, TestDispatchRule, TestClaimRule, TestGroupRule

### com.isahl.chess.audience.knight.cluster.management
- 文件数: 1
- 主要类: ClusterManagementServiceTest

### com.isahl.chess.audience.player
- 文件数: 1
- 主要类: ImModelTest

### com.isahl.chess.audience.queen.io.udp
- 文件数: 1
- 主要类: UdpCommunicationTest

### com.isahl.chess.audience.start
- 文件数: 2
- 主要类: ApplicationAudience, ApplicationAudienceTest

### com.isahl.chess.audience.testing
- 文件数: 4
- 主要类: BaseTest, Mockery, TestData, Builder

### com.isahl.chess.bishop.io.ssl
- 文件数: 10
- 主要类: SslProviderFactoryTest, CertificateWatcherTest, ReloadableSSLContextTest, SslHandShakeFilter, CertificateWatcher

### com.isahl.chess.bishop.mqtt.v5
- 文件数: 9
- 主要类: SharedSubscriptionManagerTest, MessageExpiryHandlerTest, SharedSubscriptionManagerImpl, MessageExpiryHandler, ExpirableMessage

### com.isahl.chess.bishop.protocol
- 文件数: 2
- 主要类: ProtocolContext

### com.isahl.chess.bishop.protocol.json
- 文件数: 1
- 主要类: JsonUtilTest

### com.isahl.chess.bishop.protocol.modbus
- 文件数: 1
- 主要类: ModbusConstants

### com.isahl.chess.bishop.protocol.modbus.exception
- 文件数: 1
- 主要类: ModbusException

### com.isahl.chess.bishop.protocol.modbus.function
- 文件数: 2
- 主要类: ModbusExtendedFunctions

### com.isahl.chess.bishop.protocol.modbus.master
- 文件数: 3
- 主要类: ModbusResponse, ModbusMaster, Builder, ModbusRequest

### com.isahl.chess.bishop.protocol.modbus.metrics
- 文件数: 1
- 主要类: ModbusMetrics

### com.isahl.chess.bishop.protocol.modbus.model
- 文件数: 2
- 主要类: ModbusMessage

### com.isahl.chess.bishop.protocol.modbus.retry
- 文件数: 2
- 主要类: ExponentialBackoffRetry

### com.isahl.chess.bishop.protocol.modbus.rtu
- 文件数: 2
- 主要类: ModbusRtuProtocol, ModbusRtuCodec

### com.isahl.chess.bishop.protocol.modbus.serial
- 文件数: 3
- 主要类: SerialPortConfig, Builder, ModbusRtuOverSerial, JSerialPortWrapper

### com.isahl.chess.bishop.protocol.modbus.slave
- 文件数: 4
- 主要类: ModbusSlave, Builder, ModbusSlaveConfig, Builder, DataModel

### com.isahl.chess.bishop.protocol.modbus.spi
- 文件数: 2
- 主要类: ModbusRtuProtocolHandler, ModbusTcpProtocolHandler

### com.isahl.chess.bishop.protocol.modbus.tag
- 文件数: 6
- 主要类: Tag, Builder, DataTypeConverter, TagManager

### com.isahl.chess.bishop.protocol.modbus.tcp
- 文件数: 6
- 主要类: ModbusTlsServer, ModbusTlsConfig, Builder, ModbusTlsAsyncClient, ModbusTcpCodec

### com.isahl.chess.bishop.protocol.mqtt
- 文件数: 11
- 主要类: MockNetworkOption, QttPropertyTest, X111_QttConnectTest, X11A_QttUnsubscribeTest, MqttCodecTest

### com.isahl.chess.bishop.protocol.mqtt.command
- 文件数: 10
- 主要类: X11B_QttUnsuback, X119_QttSuback, QttCommand, X116_QttPubrel, X11A_QttUnsubscribe

### com.isahl.chess.bishop.protocol.mqtt.ctrl
- 文件数: 7
- 主要类: QttControl, X112_QttConnack, X11D_QttPingresp, X111_QttConnect, X11C_QttPingreq

### com.isahl.chess.bishop.protocol.mqtt.factory
- 文件数: 1
- 主要类: QttFactory

### com.isahl.chess.bishop.protocol.mqtt.filter
- 文件数: 3
- 主要类: QttControlFilter, QttFrameFilter, QttCommandFilter

### com.isahl.chess.bishop.protocol.mqtt.model
- 文件数: 8
- 主要类: QttFrame, QttPropertySet, QttProtocol, QttContext, QttTopicAlias

### com.isahl.chess.bishop.protocol.mqtt.service
- 文件数: 4
- 主要类: ScramSha256AuthProvider, StoredCredentials, AuthResult, AuthContext, PasswordAuthProvider

### com.isahl.chess.bishop.protocol.spi
- 文件数: 3
- 主要类: ProtocolLoader

### com.isahl.chess.bishop.protocol.spi.example
- 文件数: 1
- 主要类: ExampleTextProtocolHandler

### com.isahl.chess.bishop.protocol.ws
- 文件数: 1
- 主要类: WsContext

### com.isahl.chess.bishop.protocol.ws.command
- 文件数: 1
- 主要类: X105_Text

### com.isahl.chess.bishop.protocol.ws.ctrl
- 文件数: 4
- 主要类: X101_HandShake, X104_Pong, X102_Close, X103_Ping

### com.isahl.chess.bishop.protocol.ws.features
- 文件数: 1

### com.isahl.chess.bishop.protocol.ws.filter
- 文件数: 5
- 主要类: WsTextFilter, WsFrameFilter, WsHandShakeFilter, WsControlFilter, WsProxyFilter

### com.isahl.chess.bishop.protocol.ws.model
- 文件数: 2
- 主要类: WsFrame, WsControl

### com.isahl.chess.bishop.protocol.ws.proxy
- 文件数: 1
- 主要类: WsProxyContext

### com.isahl.chess.bishop.protocol.zchat
- 文件数: 2
- 主要类: ZContext, EZContext

### com.isahl.chess.bishop.protocol.zchat.custom
- 文件数: 3
- 主要类: ZBaseMappingCustom, ZLinkCustom, ZClusterCustom

### com.isahl.chess.bishop.protocol.zchat.factory
- 文件数: 6
- 主要类: ZInnerFactory, ZServerFactory, ZConsumerFactory, ZClusterFactory, ZSymmetryFactory

### com.isahl.chess.bishop.protocol.zchat.filter
- 文件数: 4
- 主要类: ZCommandFilter, ZControlFilter, ZFrameFilter, ZEFilter

### com.isahl.chess.bishop.protocol.zchat.model.base
- 文件数: 2
- 主要类: ZFrame, ZProtocol

### com.isahl.chess.bishop.protocol.zchat.model.command
- 文件数: 4
- 主要类: X1D_PlainText, ZCommand, X1F_Exchange, X1E_Consensus

### com.isahl.chess.bishop.protocol.zchat.model.command.raft
- 文件数: 23
- 主要类: X77_RaftNotify, X78_RaftModify, X79_RaftConfirm, X84_RaftLeaseReadResp, X74_RaftReject

### com.isahl.chess.bishop.protocol.zchat.model.ctrl
- 文件数: 9
- 主要类: X0D_Error, X09_Redirect, X0A_Close, X07_SslHandShakeSend, X07_SslHandShake

### com.isahl.chess.bishop.protocol.zchat.model.ctrl.zls
- 文件数: 6
- 主要类: X03_Cipher, X02_AsymmetricPub, X01_EncryptRequest, X05_EncryptStart, X04_EncryptConfirm

### com.isahl.chess.bishop.protocol.zchat.zcrypto
- 文件数: 1
- 主要类: Encryptor

### com.isahl.chess.bishop.sort
- 文件数: 1

### com.isahl.chess.bishop.sort.mqtt
- 文件数: 1
- 主要类: MqttZSort

### com.isahl.chess.bishop.sort.ssl
- 文件数: 1
- 主要类: SslZSort

### com.isahl.chess.bishop.sort.ws
- 文件数: 1
- 主要类: WsTextZSort

### com.isahl.chess.bishop.sort.ws.proxy
- 文件数: 1
- 主要类: WsProxyZSort

### com.isahl.chess.bishop.sort.zchat
- 文件数: 2
- 主要类: ZlsZSort, ZSort

### com.isahl.chess.board.annotation
- 文件数: 2

### com.isahl.chess.board.base
- 文件数: 2

### com.isahl.chess.board.processor
- 文件数: 8
- 主要类: ZAnnotationProcessor, FactoryTranslator, SerialProcessor, SwitchBuilderTranslator, FactoryProcessor

### com.isahl.chess.board.processor.model
- 文件数: 4
- 主要类: ProcessorContext

### com.isahl.chess.board.processor.services
- 文件数: 1
- 主要类: BoardLogLoader

### com.isahl.chess.king.base.constant
- 文件数: 1

### com.isahl.chess.king.base.content
- 文件数: 4
- 主要类: ZResponse, ByteBuf, ZProgress

### com.isahl.chess.king.base.cron
- 文件数: 3
- 主要类: ScheduleHandler, TimeWheel, TickSlot, HandleTask

### com.isahl.chess.king.base.cron.features
- 文件数: 3

### com.isahl.chess.king.base.crypt.features
- 文件数: 1

### com.isahl.chess.king.base.crypt.util
- 文件数: 1
- 主要类: AesGcm

### com.isahl.chess.king.base.disruptor.components
- 文件数: 3
- 主要类: Health, Z1Processor, Z2Processor

### com.isahl.chess.king.base.disruptor.features.debug
- 文件数: 1

### com.isahl.chess.king.base.disruptor.features.event
- 文件数: 1

### com.isahl.chess.king.base.disruptor.features.flow
- 文件数: 2

### com.isahl.chess.king.base.disruptor.features.functions
- 文件数: 4

### com.isahl.chess.king.base.exception
- 文件数: 2
- 主要类: MissingParameterException, ZException

### com.isahl.chess.king.base.features
- 文件数: 8

### com.isahl.chess.king.base.features.io
- 文件数: 2

### com.isahl.chess.king.base.features.model
- 文件数: 7

### com.isahl.chess.king.base.log
- 文件数: 1
- 主要类: Logger

### com.isahl.chess.king.base.model
- 文件数: 5
- 主要类: BinarySerial, ListSerial, MapSerial, TextSerial, SetSerial

### com.isahl.chess.king.base.util
- 文件数: 17
- 主要类: JsonUtil, CryptoUtil, ASymmetricKeyPair, Pair, RegExUtil

### com.isahl.chess.king.base.util.crc
- 文件数: 2
- 主要类: CrcUtil, CrcParams, CrcEngine, CrcStream, CrcUtilTest

### com.isahl.chess.king.config
- 文件数: 2

### com.isahl.chess.king.env
- 文件数: 1
- 主要类: ZUID

### com.isahl.chess.king.metrics
- 文件数: 2
- 主要类: MetricsRegistry, ZChessMetrics

### com.isahl.chess.knight.cluster
- 文件数: 1

### com.isahl.chess.knight.cluster.config
- 文件数: 4
- 主要类: SocketConfig, SslSocketConfig, SslConfig

### com.isahl.chess.knight.cluster.features
- 文件数: 3

### com.isahl.chess.knight.cluster.management
- 文件数: 4
- 主要类: ClusterStatus, ClusterManagementServiceImpl, ClusterNode

### com.isahl.chess.knight.cluster.model
- 文件数: 1
- 主要类: ConsistentText

### com.isahl.chess.knight.engine
- 文件数: 4
- 主要类: DefaultSchedulerEngine, DefaultTaskContext, DispatchTaskContext, ClaimTaskContext, DefaultSubTaskContext

### com.isahl.chess.knight.io.ssl
- 文件数: 2
- 主要类: ZChatTlsIntegrationTest, SSLContextTest

### com.isahl.chess.knight.io.ssl.client
- 文件数: 3
- 主要类: TlsTestServer, TlsTestClient, TlsClientTest

### com.isahl.chess.knight.policy
- 文件数: 3
- 主要类: BaseRetryPolicy, RoundRobinLoadBalancePolicy

### com.isahl.chess.knight.raft
- 文件数: 1
- 主要类: RaftConfigTest

### com.isahl.chess.knight.raft.config
- 文件数: 2
- 主要类: ZRaftConfig, Uid

### com.isahl.chess.knight.raft.features
- 文件数: 7

### com.isahl.chess.knight.raft.model
- 文件数: 15
- 主要类: RaftMachine, RaftNode, RaftCongress, RaftGraph, MembershipChangeManager

### com.isahl.chess.knight.raft.model.replicate
- 文件数: 12
- 主要类: Mapper, Segment, MembershipConfig, SnapshotEntry, LogEntry

### com.isahl.chess.knight.raft.service
- 文件数: 2
- 主要类: RaftCustom, RaftPeer, ReadIndexRequest

### com.isahl.chess.knight.raft.util
- 文件数: 1
- 主要类: LongToDataSizeConverter

### com.isahl.chess.knight.scheduler.api
- 文件数: 1
- 主要类: SchedulerController, DispatchRequest, ClaimRequest, ResultRequest

### com.isahl.chess.knight.scheduler.core
- 文件数: 13
- 主要类: DispatchScheduler, RetryConfig, TaskPool, TimeoutChecker, ClaimScheduler

### com.isahl.chess.knight.scheduler.domain
- 文件数: 8
- 主要类: TaskResult, SubTaskResultEntry, Task, SubTask, NodeGroup

### com.isahl.chess.knight.scheduler.repository
- 文件数: 3

### com.isahl.chess.knight.service
- 文件数: 2
- 主要类: SchedulerServiceImpl

### com.isahl.chess.pawn.endpoint.device
- 文件数: 2
- 主要类: StateServiceTest, DeviceNode

### com.isahl.chess.pawn.endpoint.device.api.model
- 文件数: 1
- 主要类: MessageBodyTest

### com.isahl.chess.pawn.endpoint.device.config
- 文件数: 7
- 主要类: PawnIoConfig, MixCoreConfig, MqttConfig, MqttFeatureConfig, MqttAuthConfig

### com.isahl.chess.pawn.endpoint.device.db.central.config
- 文件数: 1
- 主要类: CentralJpaConfig

### com.isahl.chess.pawn.endpoint.device.db.central.model
- 文件数: 4
- 主要类: ZChatEntity, DateEntity, MsgDeliveryStatus, DeviceEntity

### com.isahl.chess.pawn.endpoint.device.db.central.repository
- 文件数: 3

### com.isahl.chess.pawn.endpoint.device.db.generator
- 文件数: 1
- 主要类: MessageCacheKeyGenerator

### com.isahl.chess.pawn.endpoint.device.db.legacy
- 文件数: 1
- 主要类: LegacyBinaryType

### com.isahl.chess.pawn.endpoint.device.db.local.config
- 文件数: 1
- 主要类: LocalJpaConfig

### com.isahl.chess.pawn.endpoint.device.db.local.model
- 文件数: 2
- 主要类: SessionEntity, MsgStateEntity

### com.isahl.chess.pawn.endpoint.device.db.local.repository
- 文件数: 2

### com.isahl.chess.pawn.endpoint.device.db.local.service
- 文件数: 2
- 主要类: StateService, MsgStateService

### com.isahl.chess.pawn.endpoint.device.model
- 文件数: 3
- 主要类: DeviceClient, ConsistentData, entry

### com.isahl.chess.pawn.endpoint.device.resource.features
- 文件数: 3

### com.isahl.chess.pawn.endpoint.device.resource.model
- 文件数: 2
- 主要类: DeviceProfile, ExpirationProfile, KeyPairProfile, MessageBody

### com.isahl.chess.pawn.endpoint.device.resource.service
- 文件数: 2
- 主要类: DeviceService, MessageService

### com.isahl.chess.pawn.endpoint.device.service
- 文件数: 5
- 主要类: LinkCustom, NodeService, QttMessageExpiryService, ExpiryTask, QttSharedSubscriptionManager

### com.isahl.chess.pawn.endpoint.device.service.plugin
- 文件数: 1
- 主要类: MessageSubscribe

### com.isahl.chess.pawn.endpoint.device.spi
- 文件数: 2

### com.isahl.chess.pawn.endpoint.device.spi.plugin
- 文件数: 4
- 主要类: WebSocketPlugin, ClusterPlugin, MQttPlugin, CachedDevice, PersistentHook

### com.isahl.chess.pawn.endpoint.device.test
- 文件数: 1
- 主要类: DeviceGeneratorTest

### com.isahl.chess.player.api.component
- 文件数: 3
- 主要类: HttpPlugin, EchoPlugin, BusinessPlugin

### com.isahl.chess.player.api.config
- 文件数: 1
- 主要类: PlayerConfig

### com.isahl.chess.player.api.controller
- 文件数: 5
- 主要类: DeviceController, ClusterController, ConsistencyController, EchoController, MessageController

### com.isahl.chess.player.api.helper
- 文件数: 1
- 主要类: XmlToJsonConverter

### com.isahl.chess.player.api.im
- 文件数: 4
- 主要类: MessageController, SendMessageRequest, AuthController, LoginRequest, GroupController

### com.isahl.chess.player.api.mock
- 文件数: 1
- 主要类: MockApi

### com.isahl.chess.player.api.model
- 文件数: 13
- 主要类: RpaAuthDo, DeviceDo, RpaTaskDO, BiddingRpaApiResponse, ClusterDo

### com.isahl.chess.player.api.service
- 文件数: 7
- 主要类: HookOpenService, AliothApiService, BiddingRpaScheduleService, BiddingRpaMessageService, ConsistencyOpenService

### com.isahl.chess.player.api.subscribe
- 文件数: 1
- 主要类: MeihangBiddingRpaSubscribe

### com.isahl.chess.player.config
- 文件数: 2
- 主要类: WebSocketConfig, JpaConfig

### com.isahl.chess.player.console
- 文件数: 2
- 主要类: MetricsController, ClusterController

### com.isahl.chess.player.domain
- 文件数: 5
- 主要类: Message, Group, UserSession, User, GroupMember

### com.isahl.chess.player.repository
- 文件数: 5

### com.isahl.chess.player.scheduler
- 文件数: 1
- 主要类: BusinessScheduler, DeviceControlHandler, AIInferenceHandler, ReportGenerationHandler

### com.isahl.chess.player.service
- 文件数: 1
- 主要类: PushService

### com.isahl.chess.queen.config
- 文件数: 7

### com.isahl.chess.queen.db.features
- 文件数: 1

### com.isahl.chess.queen.db.model
- 文件数: 1

### com.isahl.chess.queen.events.client
- 文件数: 4
- 主要类: ClientWriteDispatcher, ClientIoDispatcher, ClientReaderHandler, ClientDecodedHandler

### com.isahl.chess.queen.events.cluster
- 文件数: 4
- 主要类: DecodedDispatcher, IoDispatcher

### com.isahl.chess.queen.events.functions
- 文件数: 11
- 主要类: SocketConnected, SessionWrote, Closer, PipeEncoder, SocketConnectFailed

### com.isahl.chess.queen.events.model
- 文件数: 1
- 主要类: QEvent, QEventFactory

### com.isahl.chess.queen.events.pipe
- 文件数: 4
- 主要类: EncodedHandler, WriteDispatcher, EncodeHandler, DecodeHandler

### com.isahl.chess.queen.events.routes
- 文件数: 1

### com.isahl.chess.queen.events.server
- 文件数: 5
- 主要类: MixIoDispatcher, MixMappingHandler, MixDecodedDispatcher

### com.isahl.chess.queen.io.core.example
- 文件数: 2
- 主要类: MixManager, MixManagerTest

### com.isahl.chess.queen.io.core.features.cluster
- 文件数: 4

### com.isahl.chess.queen.io.core.features.model.channels
- 文件数: 10

### com.isahl.chess.queen.io.core.features.model.content
- 文件数: 7

### com.isahl.chess.queen.io.core.features.model.pipe
- 文件数: 8

### com.isahl.chess.queen.io.core.features.model.routes
- 文件数: 6
- 主要类: Topic, Subscribe

### com.isahl.chess.queen.io.core.features.model.session
- 文件数: 15

### com.isahl.chess.queen.io.core.features.model.session.proxy
- 文件数: 1

### com.isahl.chess.queen.io.core.features.model.session.ssl
- 文件数: 2

### com.isahl.chess.queen.io.core.features.model.session.zls
- 文件数: 2

### com.isahl.chess.queen.io.core.model
- 文件数: 2
- 主要类: BaseSort

### com.isahl.chess.queen.io.core.net.datagram
- 文件数: 2
- 主要类: BaseDatagramPeer

### com.isahl.chess.queen.io.core.net.socket
- 文件数: 11
- 主要类: AioWorker, AioPacket, BaseAioClient, AioCreator, AioSession

### com.isahl.chess.queen.io.core.net.socket.features
- 文件数: 3

### com.isahl.chess.queen.io.core.net.socket.features.client
- 文件数: 1

### com.isahl.chess.queen.io.core.net.socket.features.server
- 文件数: 1

### com.isahl.chess.queen.io.core.net.udp
- 文件数: 4
- 主要类: UdpPacket, BaseUdpClient, BaseUdpServer

### com.isahl.chess.queen.io.core.tasks
- 文件数: 2
- 主要类: ServerCore, ClientCore

### com.isahl.chess.queen.io.core.tasks.features
- 文件数: 5

### com.isahl.chess.queen.message
- 文件数: 1
- 主要类: InnerProtocol

### com.isahl.chess.rook.graphic
- 文件数: 4

### com.isahl.chess.rook.graphic.graphql
- 文件数: 5
- 主要类: BFS, DFS

### com.isahl.chess.rook.graphic.model
- 文件数: 3
- 主要类: GEdge, GNode

### com.isahl.chess.rook.storage.cache.config
- 文件数: 1
- 主要类: EhcacheConfig

### com.isahl.chess.rook.storage.cache.ehcache
- 文件数: 3
- 主要类: CacheExpiry, CacheLogger, CacheChecker

### com.isahl.chess.rook.storage.db.config
- 文件数: 3
- 主要类: RookSource, ds, RookJpaConfig, RookJpaConfigTest

### com.isahl.chess.rook.storage.db.health
- 文件数: 1
- 主要类: DatabaseHealthIndicator

### com.isahl.chess.rook.storage.db.model
- 文件数: 1
- 主要类: AuditModel

### com.isahl.chess.rook.storage.db.repository
- 文件数: 3

### com.isahl.chess.rook.storage.db.service
- 文件数: 1
- 主要类: RookProvider

### com.isahl.chess.square
- 文件数: 1
- 主要类: EdgeAgent

### com.isahl.chess.square.config
- 文件数: 1
- 主要类: EdgeConfig

### com.isahl.chess.square.model
- 文件数: 2
- 主要类: TaskResult, NodeInfo

### com.isahl.chess.square.scheduler
- 文件数: 1
- 主要类: HeartbeatScheduler

### com.isahl.chess.square.service
- 文件数: 3
- 主要类: EdgeClient, DefaultTaskExecutor

### com.securityinnovation.jNeo
- 文件数: 12
- 主要类: exists, is, list, OIDMap, provides

### com.securityinnovation.jNeo.digest
- 文件数: 5
- 主要类: defines, Digest, provides, used, implements

### com.securityinnovation.jNeo.inputstream
- 文件数: 4
- 主要类: implements, also, X982Drbg, buffers, was

### com.securityinnovation.jNeo.math
- 文件数: 7
- 主要类: implements, MGF_TP_1, provides, provides, FullPolynomial

### com.securityinnovation.jNeo.ntruencrypt
- 文件数: 2
- 主要类: represents, NtruEncryptKey, to, holds, KeyParams

### com.securityinnovation.jNeo.ntruencrypt.encoder
- 文件数: 9
- 主要类: PubKeyFormatter_PUBLIC_KEY_v1, PrivKeyFormatter_PrivateKeyListedFv1, KeyFormatterUtil, NtruEncryptKeyNativeEncoder, PrivKeyFormatter_PrivateKeyPackedFv1

### default
- 文件数: 1
- 主要类: TestAccountAuth

## 统计信息

### 类分布
- Mod*: 36 个类
- Qtt*: 14 个类
- Mes*: 12 个类
- Dev*: 11 个类
- Aio*: 10 个类
- Bas*: 10 个类
- Raf*: 10 个类
- Cli*: 9 个类
- Clu*: 8 个类
- Mix*: 8 个类