-- ============================================================================
-- Z-Chess 数据库初始化脚本
-- PostgreSQL 17 优化版
-- 
-- 注意：此脚本与 db.properties 配置保持一致
-- - 数据库名: postgres (与 docker-compose 环境变量一致)
-- - schema: isahl (与 db.properties 中的 default_schema 一致)
-- - 用户名: postgres (默认超级用户)
-- ============================================================================

-- 创建扩展（在 postgres 数据库中）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- 创建 isahl schema（与 db.properties 配置一致）
CREATE SCHEMA IF NOT EXISTS isahl;

-- 注释
COMMENT ON SCHEMA isahl IS 'Z-Chess Application Schema';
