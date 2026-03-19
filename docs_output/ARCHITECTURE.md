# Z-Chess 架构文档

## 系统架构

Z-Chess 是一个面向IoT场景的高性能通讯服务，内建基于Raft协议的分布式一致性集群。

## 模块划分

### Z-King
事件处理组件，提供基于Disruptor的事件处理框架
- 文件数: 109
- 主要类: MetricsRegistry, ZChessMetrics, ZUID, JsonUtil, CryptoUtil

### Z-Queen
核心服务组件
- 文件数: 126
- 主要类: InnerProtocol, DecodedDispatcher, IoDispatcher, EncodedHandler, WriteDispatcher

### Z-Rook
存储组件
- 文件数: 24
- 主要类: BFS, DFS, GEdge, GNode, RookSource

### Z-Bishop
消息处理组件
- 文件数: 170
- 主要类: ProtocolContext, SharedSubscriptionManagerImpl, MessageExpiryHandler, SharedSubscription, LastWillHandler

### Z-Knight
集群组件，实现Raft协议
- 文件数: 80
- 主要类: SchedulerServiceImpl, DefaultSchedulerEngine, BaseRetryPolicy, RoundRobinLoadBalancePolicy, LongToDataSizeConverter

### Z-Pawn
客户端组件
- 文件数: 47
- 主要类: DeviceNode, PawnIoConfig, MixCoreConfig, MqttConfig, MqttFeatureConfig

### Z-Player
播放器组件
- 文件数: 52
- 主要类: WebSocketConfig, JpaConfig, BusinessScheduler, PushService, Message

### Z-Audience
观察者组件
- 文件数: 94
- 主要类: ApplicationAudience, IoConsumerConfig, AudienceCacheConfig, ClientConfig, SocketConfig

### Z-Arena
竞技场/测试组件
- 文件数: 3
- 主要类: ApplicationArena, RookCacheService, RookCacheApi

### Z-Board
面板/管理组件
- 文件数: 16
- 主要类: ZAnnotationProcessor, FactoryTranslator, SerialProcessor, SwitchBuilderTranslator, FactoryProcessor

## 包依赖关系

```mermaid
graph TD
    com_isahl_chess_player_repository --> com_isahl_chess_player_domain_User
    com_isahl_chess_player_repository --> com_isahl_chess_player_domain_Message
    com_isahl_chess_player_repository --> com_isahl_chess_rook_storage_db_repository_BaseLongRepository
    com_isahl_chess_player_repository --> com_isahl_chess_player_domain_Group
    com_isahl_chess_player_repository --> com_isahl_chess_player_domain_UserSession
    com_isahl_chess_player_scheduler --> com_isahl_chess_knight_service_SchedulerService
    com_isahl_chess_player_scheduler --> com_isahl_chess_knight_engine_SchedulingRule
    com_isahl_chess_player_scheduler --> com_isahl_chess_knight_scheduler_domain_TaskResult
    com_isahl_chess_player_scheduler --> com_isahl_chess_knight_scheduler_domain_TaskStatus
    com_isahl_chess_player_scheduler --> com_isahl_chess_knight_policy_Policy
    com_isahl_chess_player_service --> com_isahl_chess_player_domain_User
    com_isahl_chess_player_service --> com_isahl_chess_player_domain_Message
    com_isahl_chess_player_service --> com_isahl_chess_player_repository_UserRepository
    com_isahl_chess_player_service --> com_isahl_chess_player_repository_GroupMemberRepository
    com_isahl_chess_player_service --> com_isahl_chess_player_repository_MessageRepository
    com_isahl_chess_player_domain --> com_isahl_chess_rook_storage_db_model_AuditModel
    com_isahl_chess_player_api_mock --> com_isahl_chess_king_config_CodeKing
    com_isahl_chess_player_api_mock --> com_isahl_chess_king_base_content_ZResponse
    com_isahl_chess_player_api_mock --> com_isahl_chess_king_base_log_Logger
    com_isahl_chess_player_api_mock --> com_isahl_chess_bishop_protocol_mqtt_factory_QttFactory
    com_isahl_chess_player_api_mock --> com_isahl_chess_knight_raft_model_replicate_LogEntry
    com_isahl_chess_player_api_component --> com_isahl_chess_pawn_endpoint_device_spi_IHandleHook
    com_isahl_chess_player_api_component --> com_isahl_chess_queen_io_core_features_model_session_IExchanger
    com_isahl_chess_player_api_component --> com_isahl_chess_king_base_log_Logger
    com_isahl_chess_player_api_component --> com_isahl_chess_queen_io_core_features_model_content_IProtocol
    com_isahl_chess_player_api_component --> com_isahl_chess_king_base_features_model_IoSerial
    com_isahl_chess_player_api_controller --> com_isahl_chess_king_base_exception_ZException
    com_isahl_chess_player_api_controller --> com_isahl_chess_player_api_model_EchoDo
    com_isahl_chess_player_api_controller --> com_isahl_chess_pawn_endpoint_device_resource_model_DeviceProfile_KeyPairProfile
    com_isahl_chess_player_api_controller --> com_isahl_chess_player_api_service_MessageOpenService
    com_isahl_chess_player_api_controller --> com_isahl_chess_pawn_endpoint_device_resource_features_IStateService
    com_isahl_chess_player_api_subscribe --> com_isahl_chess_king_base_log_Logger
    com_isahl_chess_player_api_subscribe --> com_isahl_chess_king_base_features_model_IoSerial
    com_isahl_chess_player_api_subscribe --> com_isahl_chess_player_api_service_BiddingRpaMessageService
    com_isahl_chess_player_api_subscribe --> com_isahl_chess_player_api_component_BusinessPlugin_IBusinessSubscribe
    com_isahl_chess_player_api_subscribe --> com_isahl_chess_bishop_protocol_mqtt_command_X113_QttPublish
    com_isahl_chess_player_api_model --> com_isahl_chess_pawn_endpoint_device_resource_model_MessageBody
    com_isahl_chess_player_api_model --> com_isahl_chess_king_base_util_IoUtil
    com_isahl_chess_player_api_model --> com_isahl_chess_queen_db_model_IStorage
    com_isahl_chess_player_api_model --> com_isahl_chess_king_base_model_TextSerial
    com_isahl_chess_player_api_model --> com_isahl_chess_board_annotation_ISerialGenerator
    com_isahl_chess_player_api_im --> com_isahl_chess_player_domain_User
    com_isahl_chess_player_api_im --> com_isahl_chess_player_domain_Message
    com_isahl_chess_player_api_im --> com_isahl_chess_player_domain_GroupMember
    com_isahl_chess_player_api_im --> com_isahl_chess_king_base_content_ZResponse
    com_isahl_chess_player_api_im --> com_isahl_chess_player_repository_UserRepository
    com_isahl_chess_player_api_service --> com_isahl_chess_king_base_util_IoUtil
    com_isahl_chess_player_api_service --> com_isahl_chess_knight_raft_model_RaftState
    com_isahl_chess_player_api_service --> com_isahl_chess_player_api_model_RpaTaskMessageDO
    com_isahl_chess_player_api_service --> com_isahl_chess_player_api_model_EchoDo
    com_isahl_chess_player_api_service --> com_isahl_chess_king_base_disruptor_features_functions_OperateType
    com_isahl_chess_board_processor --> com_isahl_chess_board_processor_model_Child
    com_isahl_chess_board_processor --> com_isahl_chess_board_annotation_ISerialGenerator
    com_isahl_chess_board_processor --> com_isahl_chess_board_annotation_ISerialFactory
    com_isahl_chess_board_processor --> com_isahl_chess_board_processor_model_ProcessorContext
    com_isahl_chess_board_processor_model --> com_isahl_chess_board_base_ISerial
    com_isahl_chess_king_config --> com_isahl_chess_king_base_features_ICode
    com_isahl_chess_king_base_util --> com_isahl_chess_king_config_CodeKing
    com_isahl_chess_king_base_util --> com_isahl_chess_king_base_exception_ZException
    com_isahl_chess_king_base_util --> com_isahl_chess_king_base_log_Logger
    com_isahl_chess_king_base_util --> com_isahl_chess_king_base_features_model_ITriple
    com_isahl_chess_king_base_util --> com_securityinnovation_jNeo_ntruencrypt_NtruEncryptKey
    com_isahl_chess_king_base_content --> com_isahl_chess_king_config_CodeKing
    com_isahl_chess_king_base_content --> com_isahl_chess_king_base_exception_ZException
    com_isahl_chess_king_base_content --> com_isahl_chess_king_base_util_IoUtil
    com_isahl_chess_king_base_content --> com_isahl_chess_king_base_cron_Status
    com_isahl_chess_king_base_content --> com_isahl_chess_king_base_features_ICode
    com_isahl_chess_king_base_model --> com_isahl_chess_king_base_features_model_IMapSerial
    com_isahl_chess_king_base_model --> com_isahl_chess_king_base_features_model_IoSerial
    com_isahl_chess_king_base_model --> com_isahl_chess_board_annotation_ISerialGenerator
    com_isahl_chess_king_base_model --> com_isahl_chess_king_base_features_model_IoFactory
    com_isahl_chess_king_base_model --> com_isahl_chess_board_base_ISerial
    com_isahl_chess_king_base_cron --> com_isahl_chess_king_base_log_Logger
    com_isahl_chess_king_base_cron --> com_isahl_chess_king_base_features_IValid
    com_isahl_chess_king_base_cron --> com_isahl_chess_king_base_cron_features_ICancelable
    com_isahl_chess_king_base_cron --> com_isahl_chess_king_base_cron_features_ITask
    com_isahl_chess_king_base_cron_features --> com_isahl_chess_king_base_cron_Status
    com_isahl_chess_king_base_cron_features --> com_isahl_chess_king_base_features_IValid
    com_isahl_chess_king_base_disruptor_components --> com_isahl_chess_king_base_disruptor_features_flow_IBatchHandler
    com_isahl_chess_king_base_disruptor_components --> com_isahl_chess_king_base_log_Logger
    com_isahl_chess_king_base_disruptor_components --> com_isahl_chess_king_base_disruptor_features_event_IEvent
    com_isahl_chess_king_base_disruptor_components --> com_isahl_chess_king_base_disruptor_features_debug_IHealth
```

## 核心组件交互

```mermaid
sequenceDiagram
    participant Client
    participant ZPawn as Z-Pawn
    participant ZKing as Z-King
    participant ZKnight as Z-Knight
    participant ZRook as Z-Rook
    Client->>ZPawn: 发送消息
    ZPawn->>ZKing: 提交事件
    ZKing->>ZKnight: 集群同步
    ZKnight->>ZRook: 持久化
    ZRook-->>ZKnight: 确认
    ZKnight-->>ZKing: 同步完成
    ZKing-->>ZPawn: 事件处理完成
    ZPawn-->>Client: 响应
```