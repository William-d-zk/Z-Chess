# MIT License
#
# Copyright (c) 2016~2024. Z-Chess
#
# Z-Audience Pressure Test Client Docker Image
# 独立的压测客户端，支持 MQTT/WebSocket/ZChat 协议
#

FROM bellsoft/liberica-openjdk-alpine:17.0.18-10

LABEL maintainer="Z-Chess.Cluster" \
      version="1.0.22" \
      description="Z-Audience Pressure Test Client"

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
    SPRING_PROFILES_ACTIVE=docker

# 安装必要的工具
RUN set -eux && \
    apk upgrade --no-cache && \
    apk add --no-cache \
        tzdata \
        curl \
        jq \
        ca-certificates && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo ${TZ} > /etc/timezone && \
    rm -rf /var/cache/apk/* /tmp/*

WORKDIR /app

# 复制应用 JAR
COPY target/*.jar app.jar

# 复制配置文件
COPY src/main/resources/application-docker.yml application.yml
COPY src/main/resources/audience.properties .
COPY src/main/resources/pressure-test.properties .
COPY src/main/resources/ehcache.xml .

# 创建数据目录（用于挂载测试设备凭证）
RUN mkdir -p /app/data

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:8081/actuator/health || exit 1

# 暴露 HTTP API 端口
EXPOSE 8081

# 启动命令
ENTRYPOINT ["sh", "-c", "echo 'Starting Z-Audience Pressure Test Client...' && java ${JAVA_OPTS} -jar /app/app.jar --spring.config.location=file:/app/application.yml,file:/app/audience.properties,file:/app/pressure-test.properties --spring.profiles.active=docker"]
