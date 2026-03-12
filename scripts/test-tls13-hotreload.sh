#!/bin/bash
#
# TLS 1.3 和证书热更新测试脚本
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}"
CERTS_DIR="${PROJECT_DIR}/test-certs-tls13"
LOG_FILE="${PROJECT_DIR}/tls13-test.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1" | tee -a "${LOG_FILE}"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "${LOG_FILE}"
}

# 清理函数
cleanup() {
    log_info "Cleaning up..."
    if [ -n "${SERVER_PID}" ]; then
        kill "${SERVER_PID}" 2>/dev/null || true
        wait "${SERVER_PID}" 2>/dev/null || true
    fi
    rm -rf "${CERTS_DIR}"
}

trap cleanup EXIT

# 生成测试证书
generate_certs() {
    log_info "Generating test certificates..."
    mkdir -p "${CERTS_DIR}"
    
    # CA 证书
    openssl req -x509 -newkey rsa:2048 -keyout "${CERTS_DIR}/ca-key.pem" \
        -out "${CERTS_DIR}/ca-cert.pem" -days 365 -nodes \
        -subj "/CN=Test CA/O=Z-Chess/C=CN" 2>/dev/null
    
    # 服务器证书 1 (初始)
    openssl req -newkey rsa:2048 -keyout "${CERTS_DIR}/server1-key.pem" \
        -out "${CERTS_DIR}/server1-csr.pem" -nodes \
        -subj "/CN=server1/O=Z-Chess/C=CN" 2>/dev/null
    
    openssl x509 -req -in "${CERTS_DIR}/server1-csr.pem" \
        -CA "${CERTS_DIR}/ca-cert.pem" -CAkey "${CERTS_DIR}/ca-key.pem" \
        -CAcreateserial -out "${CERTS_DIR}/server1-cert.pem" -days 365 2>/dev/null
    
    # 转换为 PKCS12
    openssl pkcs12 -export -in "${CERTS_DIR}/server1-cert.pem" \
        -inkey "${CERTS_DIR}/server1-key.pem" \
        -certfile "${CERTS_DIR}/ca-cert.pem" \
        -out "${CERTS_DIR}/server1.p12" -password pass:changeit
    
    # 服务器证书 2 (用于热更新测试)
    openssl req -newkey rsa:2048 -keyout "${CERTS_DIR}/server2-key.pem" \
        -out "${CERTS_DIR}/server2-csr.pem" -nodes \
        -subj "/CN=server2/O=Z-Chess/C=CN" 2>/dev/null
    
    openssl x509 -req -in "${CERTS_DIR}/server2-csr.pem" \
        -CA "${CERTS_DIR}/ca-cert.pem" -CAkey "${CERTS_DIR}/ca-key.pem" \
        -CAcreateserial -out "${CERTS_DIR}/server2-cert.pem" -days 365 2>/dev/null
    
    openssl pkcs12 -export -in "${CERTS_DIR}/server2-cert.pem" \
        -inkey "${CERTS_DIR}/server2-key.pem" \
        -certfile "${CERTS_DIR}/ca-cert.pem" \
        -out "${CERTS_DIR}/server2.p12" -password pass:changeit
    
    # TrustStore
    keytool -import -trustcacerts -keystore "${CERTS_DIR}/trust.p12" \
        -storepass changeit -alias ca -file "${CERTS_DIR}/ca-cert.pem" -noprompt
    
    log_info "Test certificates generated in ${CERTS_DIR}"
}

# 测试 TLS 1.3 连接
test_tls13() {
    local port=$1
    log_info "Testing TLS 1.3 connection on port ${port}..."
    
    local output
    output=$(echo | openssl s_client -connect localhost:${port} \
        -CAfile "${CERTS_DIR}/ca-cert.pem" -tls1_3 2>&1) || true
    
    if echo "$output" | grep -q "verify return:1"; then
        log_info "✅ TLS 1.3 connection successful"
        echo "$output" | grep -E "Protocol|Cipher" | head -2
        return 0
    else
        log_error "❌ TLS 1.3 connection failed"
        echo "$output" | tail -20
        return 1
    fi
}

# 获取证书 CN
check_cert_cn() {
    local port=$1
    local expected_cn=$2
    
    log_info "Checking certificate CN (expecting: ${expected_cn})..."
    
    local cn
    cn=$(echo | openssl s_client -connect localhost:${port} \
        -CAfile "${CERTS_DIR}/ca-cert.pem" 2>/dev/null | \
        openssl x509 -noout -subject 2>/dev/null | \
        grep -oP 'CN=\K[^/]+') || true
    
    if [ "$cn" = "$expected_cn" ]; then
        log_info "✅ Certificate CN correct: ${cn}"
        return 0
    else
        log_error "❌ Certificate CN mismatch: got '${cn}', expected '${expected_cn}'"
        return 1
    fi
}

# 主测试流程
main() {
    log_info "========================================="
    log_info "TLS 1.3 and Hot Reload Test"
    log_info "========================================="
    
    # 生成证书
    generate_certs
    
    # 创建测试配置文件
    cat > "${PROJECT_DIR}/test-tls13-config.yaml" << EOF
z:
  chess:
    pawn:
      io:
        cluster:
          enabled: true
          tls13-enabled: true
          hot-reload-enabled: true
          hot-reload-debounce-ms: 2000
          key-store-path: ${CERTS_DIR}/server1.p12
          key-store-password: changeit
          trust-store-path: ${CERTS_DIR}/trust.p12
          trust-store-password: changeit
          client-auth: false
          protocol: TLSv1.2

server:
  port: 2808

logging:
  level:
    io.bishop.ssl: DEBUG
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
EOF
    
    log_info "Building project..."
    cd "${PROJECT_DIR}"
    mvn clean package -pl Z-Arena -am -DskipTests -q
    
    log_info "Starting Z-Chess Arena with TLS 1.3..."
    java -Xmx512m \
        -Dspring.profiles.active=local-tls \
        -Dlogging.level.com.isahl.chess=INFO \
        -jar "${PROJECT_DIR}/Z-Arena/target/gateway.z-arena-1.0.19.jar" \
        --spring.config.location="${PROJECT_DIR}/test-tls13-config.yaml" &
    
    SERVER_PID=$!
    
    log_info "Waiting for server to start..."
    for i in {1..30}; do
        if nc -z localhost 2883 2>/dev/null; then
            log_info "Server started on port 2883"
            break
        fi
        sleep 1
    done
    
    # 测试 1: TLS 1.3 连接
    log_info ""
    log_info "Test 1: TLS 1.3 Connection"
    test_tls13 2883
    
    # 测试 2: 检查初始证书
    log_info ""
    log_info "Test 2: Initial Certificate (server1)"
    check_cert_cn 2883 "server1"
    
    # 测试 3: 证书热更新
    log_info ""
    log_info "Test 3: Certificate Hot Reload"
    log_info "Replacing certificate (server1 -> server2)..."
    
    cp "${CERTS_DIR}/server2.p12" "${CERTS_DIR}/server1.p12"
    
    log_info "Waiting for hot reload (debounce: 2s + processing)..."
    sleep 5
    
    # 检查新证书
    check_cert_cn 2883 "server2"
    
    log_info ""
    log_info "========================================="
    log_info "All tests passed! ✅"
    log_info "========================================="
}

# 运行测试
main "$@"
