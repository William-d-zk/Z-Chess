#!/bin/bash
# SSL Provider 测试脚本
# 用于验证 SSL Provider 的自动降级机制

set -e

echo "====================================="
echo "Z-Chess SSL Provider 测试脚本"
echo "====================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: Java 未安装${NC}"
    exit 1
fi

echo "Java 版本:"
java -version 2>&1 | head -1

echo ""
echo "====================================="
echo "测试 1: 自动检测模式"
echo "====================================="
mvn test -pl Z-Bishop -Dtest=SslProviderFactoryTest -q 2>&1 | grep -E "(INFO|Current Provider|Tests run)" || true

echo ""
echo "====================================="
echo "测试 2: 强制使用 JDK Default"
echo "====================================="
mvn test -pl Z-Bishop -Dtest=SslProviderFactoryTest -Dssl.provider.force=jdk -q 2>&1 | grep -E "(INFO|Current Provider|Tests run)" || true

echo ""
echo "====================================="
echo "WolfSSL 安装检查"
echo "====================================="

# 检查 wolfssl 库
if ldconfig -p 2>/dev/null | grep -q wolfssl || \
   ls /usr/local/lib/libwolfssl* 2>/dev/null || \
   ls /usr/lib/libwolfssl* 2>/dev/null || \
   ls /opt/homebrew/lib/libwolfssl* 2>/dev/null; then
    echo -e "${GREEN}✓ WolfSSL 库已安装${NC}"
else
    echo -e "${YELLOW}⚠ WolfSSL 库未安装${NC}"
    echo "  安装指南:"
    echo "  - Ubuntu/Debian: sudo apt-get install libwolfssl-dev"
    echo "  - macOS: brew install wolfssl"
    echo "  - 或从源码编译安装"
fi

echo ""
echo "====================================="
echo "系统 SSL 库搜索路径"
echo "====================================="
echo "LD_LIBRARY_PATH: ${LD_LIBRARY_PATH:-"(not set)"}"
echo "DYLD_LIBRARY_PATH: ${DYLD_LIBRARY_PATH:-"(not set)"}"

echo ""
echo "====================================="
echo "Maven 依赖检查"
echo "====================================="
mvn dependency:tree -pl Z-Bishop 2>&1 | grep -E "(wolfssl|netty-tcnative)" || echo -e "${YELLOW}⚠ SSL Provider 依赖未找到${NC}"

echo ""
echo "====================================="
echo "SSL Provider 配置指南"
echo "====================================="
echo "启动参数示例:"
echo "  1. 强制 WolfSSL:   java -Dssl.provider.force=wolfssl -jar app.jar"
echo "  2. 强制 OpenSSL:   java -Dssl.provider.force=openssl -jar app.jar"
echo "  3. 强制 JDK:       java -Dssl.provider.force=jdk -jar app.jar"
echo "  4. 禁用 WolfSSL:   java -Dssl.provider.disableWolfSSL=true -jar app.jar"
echo "  5. 调试模式:       java -Dssl.debug=true -jar app.jar"

echo ""
echo -e "${GREEN}测试完成!${NC}"
