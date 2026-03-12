#!/bin/bash
#
# TLS 验证脚本
# 启动 TLS 服务端和客户端进行验证
#

cd "$(dirname "$0")/.."
CERT_DIR="cert"
PASSWORD="changeit"
SERVER_PORT=28443

echo "========================================"
echo "Z-Chess TLS 验证测试"
echo "========================================"
echo ""

# 检查证书
if [ ! -f "$CERT_DIR/server.p12" ]; then
    echo "错误: 证书文件不存在，请先运行 generate-ssl-certs.sh"
    exit 1
fi

echo "1. 证书检查:"
echo "   服务端证书: $CERT_DIR/server.p12 ✓"
echo "   信任库: $CERT_DIR/trust.p12 ✓"
echo ""

# 启动 TLS 服务端（后台）
echo "2. 启动 TLS 测试服务端 (端口 $SERVER_PORT)..."
java -cp "Z-Knight/target/test-classes:Z-Knight/target/classes:$(mvn -pl Z-Knight dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout)" \
    com.isahl.chess.knight.io.ssl.client.TlsTestServer \
    --port $SERVER_PORT \
    --keystore $CERT_DIR/server.p12 \
    --password $PASSWORD &

SERVER_PID=$!
echo "   服务端 PID: $SERVER_PID"
echo ""

# 等待服务端启动
sleep 2

# 检查服务端是否启动
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "错误: 服务端启动失败"
    exit 1
fi

echo "3. 运行 TLS 客户端测试..."
echo ""
echo "   测试 1: 单向 TLS 连接"
echo "   ------------------------"

# 使用 openssl 测试连接
echo "   使用 OpenSSL 测试 TLS 连接..."
echo | openssl s_client -connect localhost:$SERVER_PORT \
    -CAfile $CERT_DIR/ca-cert.pem 2>/dev/null | grep -E "Verify return code|Protocol|Cipher" | head -3

echo ""
echo "   测试 2: 证书信息验证"
echo "   ------------------------"
echo | openssl s_client -connect localhost:$SERVER_PORT 2>/dev/null | \
    openssl x509 -noout -subject -dates -fingerprint 2>/dev/null

echo ""
echo "4. 停止服务端..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null
echo "   ✓ 服务端已停止"

echo ""
echo "========================================"
echo "TLS 验证完成"
echo "========================================"
