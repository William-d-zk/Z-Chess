#!/bin/bash
#
# TLS MQTT 链路验证脚本
# 用于独立验证 TLS MQTT 连接
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/../../test/docker/cert"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
print_error() { echo -e "${RED}[FAIL]${NC} $1"; }

echo "========================================"
echo "   TLS MQTT 链路验证"
echo "========================================"
echo ""

# 检查证书
print_info "检查 TLS 证书..."
if [ ! -f "$CERT_DIR/server.p12" ]; then
    print_error "证书不存在: $CERT_DIR/server.p12"
    echo "请先运行: ./generate-ssl-certs.sh"
    exit 1
fi
print_success "证书存在"

# 验证证书内容
echo ""
print_info "证书信息:"
openssl x509 -in "$CERT_DIR/ca-cert.pem" -noout -subject -dates 2>/dev/null | sed 's/^/  /'

# 使用 OpenSSL 验证 TLS 连接
echo ""
print_info "测试 1: OpenSSL TLS 握手验证"
if echo | timeout 5 openssl s_client -connect localhost:8883 \
    -CAfile "$CERT_DIR/ca-cert.pem" 2>/dev/null | grep -q "Verify return code: 0"; then
    print_success "TLS 握手成功，证书验证通过"
else
    print_error "TLS 握手失败或证书验证未通过"
    echo "请确保 Z-Chess 服务已启动并监听 8883 端口"
fi

# 获取 TLS 连接详情
echo ""
print_info "TLS 连接详情:"
echo | openssl s_client -connect localhost:8883 -CAfile "$CERT_DIR/ca-cert.pem" 2>/dev/null | \
    grep -E "Protocol|Cipher|Verify" | sed 's/^/  /' || true

# 使用 mosquitto 测试 MQTT 连接（如果可用）
echo ""
print_info "测试 2: MQTT over TLS 连接测试"
if command -v mosquitto_pub &> /dev/null; then
    if mosquitto_pub -h localhost -p 8883 -t test/tls -m "test" \
        --cafile "$CERT_DIR/ca-cert.pem" -q 1 2>/dev/null; then
        print_success "MQTT TLS 发布测试通过"
    else
        print_error "MQTT TLS 发布测试失败"
    fi
else
    print_info "mosquitto 客户端未安装，跳过 MQTT 测试"
fi

# 检查 Java SSL 连接
echo ""
print_info "测试 3: Java SSLContext 验证"
java -cp /tmp -version 2>/dev/null && {
    cat > /tmp/TlsVerify.java << 'JAVA_EOF'
import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class TlsVerify {
    public static void main(String[] args) throws Exception {
        String certDir = System.getenv("CERT_DIR");
        if (certDir == null) certDir = ".";
        
        // 加载信任库
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(certDir + "/trust.p12")) {
            trustStore.load(fis, "changeit".toCharArray());
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);
        
        SSLSocketFactory factory = sslContext.getSocketFactory();
        
        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8883)) {
            socket.setSoTimeout(5000);
            socket.startHandshake();
            
            SSLSession session = socket.getSession();
            System.out.println("  协议: " + session.getProtocol());
            System.out.println("  加密套件: " + session.getCipherSuite());
            System.out.println("  验证通过");
        }
    }
}
JAVA_EOF

    cd /tmp && javac TlsVerify.java 2>/dev/null && \
    CERT_DIR="$CERT_DIR" java TlsVerify 2>/dev/null && \
    print_success "Java SSLContext 验证通过" || \
    print_error "Java SSLContext 验证失败"
} || print_info "Java 不可用，跳过 Java SSL 测试"

echo ""
echo "========================================"
echo "   验证完成"
echo "========================================"
