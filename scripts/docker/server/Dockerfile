# MIT License
#
# Copyright (c) 2016~2024. Z-Chess
#
# Z-Chess Arena Docker Image (Multi-Arch)
# 支持 AMD64 (x86_64) 和 ARM64 (aarch64)
#
# 特性:
# - 使用官方 sqlite-jdbc，自动适配不同架构
# - 无需手动管理 native 库文件
#
# 基础镜像选择 (按优先级):
# 1. bellsoft/liberica-openjdk-alpine:17 - Liberica JDK (推荐，体积小)
# 2. eclipse-temurin:17-jdk-alpine - Eclipse Adoptium (广泛可用)
# 3. amazoncorretto:17-alpine - Amazon Corretto (AWS优化)

FROM bellsoft/liberica-openjdk-alpine:17.0.18-10

LABEL maintainer="Z-Chess.Cluster" \
      version="1.0.22" \
      description="Z-Chess.Arena Cluster Node"

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

# 设置权限
RUN chmod 644 /app/app.jar /app/application.properties && \
    mkdir -p /app/logs /root/Z-Chess

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

# 端口: HTTP, Debug, MQTT, MQTT-TLS
EXPOSE 8080 8000 1883 2883

# 启动命令
ENTRYPOINT ["sh", "-c", " \
    echo 'Starting Z-Chess Arena...'; \
    echo 'Config: /app/application.properties'; \
    java \${JAVA_OPTS} \
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 \
        -jar /app/app.jar \
"]
