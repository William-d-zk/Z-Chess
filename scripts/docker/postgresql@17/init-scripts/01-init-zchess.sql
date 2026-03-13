-- ============================================================================
-- Z-Chess 数据库初始化脚本
-- PostgreSQL 17 优化版
-- 
-- 执行时机: PostgreSQL 首次初始化时自动执行
-- 执行用户: 由 POSTGRES_USER 环境变量指定 (chess)
-- 目标数据库: 由 POSTGRES_DB 环境变量指定 (isahl_9.x)
--
-- 配置对应关系:
-- - 数据库名: isahl_9.x (与 application.properties 中 postgresql.database 一致)
-- - schema: isahl (与 db.properties 中 hibernate.default_schema 一致)
-- - 用户名: chess (与 application.properties 中 postgresql.username 一致)
-- ============================================================================

\echo '>>> 开始初始化 Z-Chess 数据库...'

-- ============================================================================
-- 1. 创建扩展 (在当前数据库中)
-- ============================================================================
\echo '>>> 创建 PostgreSQL 扩展...'

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- ============================================================================
-- 2. 创建 Schema
-- ============================================================================
\echo '>>> 创建 isahl schema...'

CREATE SCHEMA IF NOT EXISTS isahl;

-- 为 schema 添加注释
COMMENT ON SCHEMA isahl IS 'Z-Chess Application Schema - 与 hibernate.default_schema 配置一致';

-- ============================================================================
-- 3. 设置搜索路径
-- ============================================================================
\echo '>>> 配置数据库搜索路径...'

-- 为当前用户设置默认搜索路径
ALTER ROLE "chess" SET search_path TO isahl, public;

-- ============================================================================
-- 4. Schema 权限配置
-- ============================================================================
\echo '>>> 配置 schema 权限...'

-- 授予 chess 用户对 isahl schema 的所有权限
GRANT ALL ON SCHEMA isahl TO "chess";

-- 设置默认权限：未来在 isahl schema 中创建的表自动授权
ALTER DEFAULT PRIVILEGES IN SCHEMA isahl GRANT ALL ON TABLES TO "chess";
ALTER DEFAULT PRIVILEGES IN SCHEMA isahl GRANT ALL ON SEQUENCES TO "chess";
ALTER DEFAULT PRIVILEGES IN SCHEMA isahl GRANT ALL ON FUNCTIONS TO "chess";

-- ============================================================================
-- 5. 验证配置
-- ============================================================================
\echo '>>> 验证初始化结果...'

-- 显示当前数据库
SELECT current_database() AS "当前数据库";

-- 显示当前用户
SELECT current_user AS "当前用户";

-- 显示已创建的扩展
SELECT extname AS "已安装扩展" FROM pg_extension WHERE extname IN ('uuid-ossp', 'pgcrypto', 'pg_stat_statements');

-- 显示 schema 信息
SELECT schema_name, schema_owner FROM information_schema.schemata WHERE schema_name = 'isahl';

\echo '>>> Z-Chess 数据库初始化完成!'
\echo '>>> 数据库: isahl_9.x'
\echo '>>> 用户: chess'
\echo '>>> Schema: isahl'
\echo '>>> 认证: 集群网段 172.30.10.0/24 和 172.30.11.0/24 使用 trust 策略'
