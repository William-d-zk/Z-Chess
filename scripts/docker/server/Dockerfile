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

ARG WOLFSSL_VERSION=5.8.4
ARG WOLFSSL_JNI_VERSION=1.16.0
ARG MAIN_CLASS=com.isahl.chess.arena.start.ApplicationArena

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
    SPRING_PROFILES_ACTIVE=dev \
    POSTGRES_PASSWORD=chess123 \
    WOLFSSL_VERSION=${WOLFSSL_VERSION} \
    WOLFSSL_JNI_VERSION=${WOLFSSL_JNI_VERSION} \
    MAIN_CLASS=${MAIN_CLASS}

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

ARG JAR_FILE=Z-Arena/target/*.jar
COPY ${JAR_FILE} app.jar

COPY scripts/docker/config/application-docker.properties application.properties

COPY scripts/docker/wolfssl/ /tmp/wolfssl/

RUN OS=$(uname -s | tr '[:upper:]' '[:lower:]') && \
    ARCH=$(cat /etc/apk/arch 2>/dev/null || uname -m) && \
    echo "Detected: OS=${OS}, Arch=${ARCH}" && \
    mkdir -p /app/lib /usr/local/lib && \
    if [ "${ARCH}" = "aarch64" ] || [ "${ARCH}" = "arm64" ]; then \
        ARCH_DIR="linux/arm64"; \
    elif [ "${ARCH}" = "x86_64" ] || [ "${ARCH}" = "amd64" ]; then \
        ARCH_DIR="linux/amd64"; \
    else \
        ARCH_DIR=""; \
    fi && \
    echo "Looking for WolfSSL in: ${ARCH_DIR}" && \
    WOLFSSL_JAR="wolfssl-jsse-${WOLFSSL_JNI_VERSION}.jar" && \
    if [ -n "${ARCH_DIR}" ] && [ -f "/tmp/wolfssl/${ARCH_DIR}/${WOLFSSL_JAR}" ]; then \
        echo "Using WolfSSL from ${ARCH_DIR}"; \
        cp /tmp/wolfssl/${ARCH_DIR}/*.jar /app/lib/; \
        cp /tmp/wolfssl/${ARCH_DIR}/*.so* /usr/local/lib/ 2>/dev/null || true; \
        cp /tmp/wolfssl/${ARCH_DIR}/*.dylib* /usr/local/lib/ 2>/dev/null || true; \
    else \
        echo "Warning: No WolfSSL files found for ${OS}/${ARCH}, will use JDK SSL"; \
    fi && \
    rm -rf /tmp/wolfssl && \
    if ls /app/lib/wolfssl*.jar 1>/dev/null 2>&1; then \
        echo "WolfSSL files copied successfully"; \
        ls -la /app/lib/; \
    else \
        echo "Using JDK SSL only"; \
    fi

RUN chmod 644 /app/app.jar /app/application.properties && \
    mkdir -p /app/logs /root/Z-Chess

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

EXPOSE 8080 8000 1883 2883

COPY scripts/docker/server/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]