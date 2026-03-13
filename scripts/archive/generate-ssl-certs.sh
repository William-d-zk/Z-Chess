#!/bin/bash
#
# MIT License
#
# Copyright (c) 2016~2024. Z-Chess
#
# 生成 Z-Chess TLS 证书脚本
# 用法: ./generate-ssl-certs.sh [环境] [密码]
# 环境: dev | test | prod (默认: dev)
# 密码: 密钥库密码 (默认: changeit)
#

set -e

ENV=${1:-dev}
CERT_DIR="cert"

# ⚠️ 安全要求: 必须设置强密码
if [ -z "$2" ]; then
    echo "错误: 必须提供密钥库密码作为第二个参数"
    echo "用法: ./generate-ssl-certs.sh [环境] [密码]"
    echo "示例: ./generate-ssl-certs.sh prod \$(openssl rand -base64 32)"
    exit 1
fi
PASSWORD=$2

# 证书有效期设置（默认365天，最少90天）
DAYS=${CERT_DAYS:-365}
if [ "$DAYS" -lt 90 ]; then
    echo "警告: 证书有效期不应少于90天，已调整为90天"
    DAYS=90
fi

# 根据环境调整证书有效期（生产环境建议90天，最长不超过365天）
if [ "$ENV" = "prod" ]; then
    # 生产环境: 90天，符合安全最佳实践
    DAYS=${CERT_DAYS:-90}
    echo "生产环境模式: 证书有效期设置为 $DAYS 天"
elif [ "$ENV" = "test" ]; then
    # 测试环境: 180天
    DAYS=${CERT_DAYS:-180}
    echo "测试环境模式: 证书有效期设置为 $DAYS 天"
else
    # 开发环境: 365天
    DAYS=${CERT_DAYS:-365}
    echo "开发环境模式: 证书有效期设置为 $DAYS 天"
fi

echo "========================================="
echo "Z-Chess TLS 证书生成脚本"
echo "环境: $ENV"
echo "有效期: $DAYS 天"
echo "========================================="
echo ""
echo "⚠️  安全提示:"
echo "   - 请妥善保管密钥库密码"
echo "   - 私钥文件(*-key.pem)和密钥库(*.p12)权限已设置为 600"
echo "   - 证书过期前30天请重新生成"
echo ""

# 创建证书目录
mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

# 清理旧证书（可选）
read -p "是否清理现有证书? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -f *.pem *.p12 *.cer *.csr *.srl
    echo "已清理现有证书"
fi

echo ""
echo "[1/6] 生成 CA 私钥和证书..."
openssl genrsa -out ca-key.pem 4096 2>/dev/null
openssl req -x509 -new -nodes -key ca-key.pem -sha256 -days 1825 \
    -subj "/CN=Z-Chess $ENV CA/O=Z-Chess/CN=CN" \
    -out ca-cert.pem

echo "[2/6] 生成服务端证书..."
openssl genrsa -out server-key.pem 2048 2>/dev/null
openssl req -new -key server-key.pem \
    -subj "/CN=server.z-chess.local/O=Z-Chess/C=CN" \
    -out server.csr
openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem \
    -CAcreateserial -out server-cert.pem -days $DAYS -sha256

echo "[3/6] 生成客户端证书..."
openssl genrsa -out client-key.pem 2048 2>/dev/null
openssl req -new -key client-key.pem \
    -subj "/CN=client.z-chess.local/O=Z-Chess/C=CN" \
    -out client.csr
openssl x509 -req -in client.csr -CA ca-cert.pem -CAkey ca-key.pem \
    -CAcreateserial -out client-cert.pem -days $DAYS -sha256

echo "[4/6] 生成集群证书..."
openssl genrsa -out cluster-key.pem 2048 2>/dev/null
openssl req -new -key cluster-key.pem \
    -subj "/CN=cluster.z-chess.local/O=Z-Chess/C=CN" \
    -out cluster.csr
openssl x509 -req -in cluster.csr -CA ca-cert.pem -CAkey ca-key.pem \
    -CAcreateserial -out cluster-cert.pem -days $DAYS -sha256

echo "[5/6] 生成 PKCS12 密钥库..."
# 服务端密钥库
openssl pkcs12 -export \
    -in server-cert.pem -inkey server-key.pem \
    -certfile ca-cert.pem -out server.p12 \
    -name server -password pass:$PASSWORD

# 客户端密钥库
openssl pkcs12 -export \
    -in client-cert.pem -inkey client-key.pem \
    -certfile ca-cert.pem -out client.p12 \
    -name client -password pass:$PASSWORD

# 集群密钥库
openssl pkcs12 -export \
    -in cluster-cert.pem -inkey cluster-key.pem \
    -certfile ca-cert.pem -out cluster.p12 \
    -name cluster -password pass:$PASSWORD

# 信任库（包含 CA 证书）
keytool -import -alias ca -file ca-cert.pem \
    -keystore trust.p12 -storetype PKCS12 -storepass $PASSWORD -noprompt

# 集群信任库（复制）
cp trust.p12 cluster-trust.p12

echo "[6/6] 设置文件权限..."
chmod 600 *.p12
chmod 600 *-key.pem
chmod 644 ca-cert.pem *-cert.pem

echo ""
echo "========================================="
echo "证书生成完成！"
echo "========================================="
echo ""
echo "生成的文件:"
echo "  CA 证书:     ca-cert.pem"
echo "  服务端:      server.p12"
echo "  客户端:      client.p12"
echo "  集群:        cluster.p12"
echo "  信任库:      trust.p12, cluster-trust.p12"
echo ""
echo "配置参数:"
echo "  密码:        $PASSWORD"
echo "  有效期:      $DAYS 天"
echo ""
echo "Docker 使用示例:"
echo "  docker run -d \\"
echo "    -v \$(pwd)/$CERT_DIR:/app/cert:ro \\"
echo "    -e SSL_ENABLED=true \\"
echo "    -e SSL_KEYSTORE_PASSWORD=$PASSWORD \\"
echo "    -p 1883:1883 \\"
echo "    z-chess:latest"
echo ""
echo "验证证书:"
echo "  openssl pkcs12 -info -in server.p12 -noout"
echo "  keytool -list -v -keystore trust.p12 -storepass $PASSWORD"
echo ""

# 显示证书信息
echo "服务端证书信息:"
openssl x509 -in server-cert.pem -noout -subject -dates -fingerprint
