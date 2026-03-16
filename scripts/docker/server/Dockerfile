# MIT License
#
# Copyright (c) 2016~2024. Z-Chess
#
# Z-Chess Arena Docker Image (Multi-Arch)
# 支持 Linux AMD64 (x86_64) 和 ARM64 (aarch64)
#
# 特性:
# - 使用官方 sqlite-jdbc，自动适配不同架构
# - 预装 WolfSSL 高性能 SSL/TLS (按 OS/Arch 放置)
#
# 基础镜像:
# bellsoft/liberica-openjdk-alpine:17
#
# WolfSSL 编译:
# 1. 在对应 OS/Arch 环境中编译 wolfssl
# 2. 将编译产物放入 scripts/docker/wolfssl/<os>/<arch>/ 目录
# 3. 构建镜像时自动复制对应架构的文件
#
# 需要的文件:
#   - libwolfssl.so / libwolfssl.dylib
#   - libwolfssljni.so / libwolfssljni.dylib
#   - wolfssl-*.jar
#   - wolfssl-jsse-*.jar

FROM bellsoft/liberica-openjdk-alpine:17.0.18-10

LABEL maintainer="Z-Chess.Cluster" \
      version="1.0.22" \
      description="Z-Chess.Arena Cluster Node with WolfSSL"

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
    SPRING_PROFILES_ACTIVE=dev \
    POSTGRES_PASSWORD=chess123

# 安装依赖
RUN set -eux && \
    apk upgrade --no-cache && \
    apk add --no-cache \
        sqlite \
        curl \
        jq \
        ca-certificates \
        tzdata && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo ${TZ} > /etc/timezone && \
    rm -rf /var/cache/apk/* /tmp/*

WORKDIR /app

# 复制应用 JAR (从项目根目录构建)
ARG JAR_FILE=Z-Arena/target/*.jar
COPY ${JAR_FILE} app.jar

# 复制配置文件到工作目录
COPY scripts/docker/config/application-docker.properties application.properties

# 复制 WolfSSL 目录（所有架构）
COPY scripts/docker/wolfssl/ /tmp/wolfssl/

# 检测 OS/Arch 并复制对应的 WolfSSL 文件
# 支持: linux/arm64, linux/amd64
RUN OS=$(uname -s | tr '[:upper:]' '[:lower:]') && \
    ARCH=$(cat /etc/apk/arch 2>/dev/null || uname -m) && \
    echo "Detected: OS=${OS}, Arch=${ARCH}" && \
    mkdir -p /app/lib && \
    # 映射架构名称
    if [ "${ARCH}" = "aarch64" ] || [ "${ARCH}" = "arm64" ]; then \
        ARCH_DIR="linux/arm64"; \
    elif [ "${ARCH}" = "x86_64" ] || [ "${ARCH}" = "amd64" ]; then \
        ARCH_DIR="linux/amd64"; \
    else \
        ARCH_DIR=""; \
    fi && \
    echo "Looking for WolfSSL in: ${ARCH_DIR}" && \
    if [ -n "${ARCH_DIR}" ] && [ -f "/tmp/wolfssl/${ARCH_DIR}/wolfssl-jsse-1.16.0.jar" ]; then \
        echo "Using WolfSSL from ${ARCH_DIR}"; \
        cp /tmp/wolfssl/${ARCH_DIR}/*.jar /app/lib/; \
        cp /tmp/wolfssl/${ARCH_DIR}/*.so* /usr/local/lib/ 2>/dev/null || true; \
        cp /tmp/wolfssl/${ARCH_DIR}/*.dylib* /usr/local/lib/ 2>/dev/null || true; \
    else \
        echo "Warning: No WolfSSL files found for ${OS}/${ARCH}"; \
    fi && \
    ldconfig 2>/dev/null || true && \
    if ls /app/lib/wolfssl*.jar 1>/dev/null 2>&1; then \
        echo "WolfSSL files copied successfully"; \
        ls -la /app/lib/; \
    else \
        echo "Warning: No WolfSSL JARs found, will use JDK SSL"; \
    fi

# 设置权限
RUN chmod 644 /app/app.jar /app/application.properties && \
    mkdir -p /app/logs /root/Z-Chess

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

# 端口: HTTP, Debug, MQTT, MQTT-TLS
EXPOSE 8080 8000 1883 2883

# 启动命令
CMD ["sh", "-c", " \
    echo 'Starting Z-Chess Arena...'; \
    echo 'Config: /app/application.properties'; \
    if ls /app/lib/wolfssl*.jar 1>/dev/null 2>&1; then \
        echo 'Using WolfSSL for TLS acceleration'; \
        WOLFSSL_CP=$(ls /app/lib/wolfssl*.jar | tr '\\n' ':'); \
        exec java ${JAVA_OPTS} -cp \"app.jar:${WOLFSSL_CP}\" \
            -Djava.library.path=/usr/local/lib \
            -jar app.jar; \
    else \
        echo 'Using JDK SSL'; \
        exec java ${JAVA_OPTS} -jar app.jar; \
    fi \
"]
