# dev 备注
JPA 存储时 entity 中存在 enum 项目需要加入
`@Enumerated(EnumType.STRING)`注解，否则数据库中将以数字进行存储，虽然会增加存储成本
这对未来修订枚举非常不利，无法实现向前兼容，所以持久化时，必须加入此注解