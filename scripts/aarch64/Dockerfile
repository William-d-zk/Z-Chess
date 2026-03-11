# MIT License
#
# Copyright (c) 2016~2024. Z-Chess
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# ============================================================================
# Z-Chess Arena Docker Image (ARM64)
# 
# 升级说明：
# - 基础镜像：Alpine Linux 3.20 (内核升级)
# - OpenJDK：17 (Liberica JDK)
# - SQLite：最新稳定版
# ============================================================================

# 生产环境镜像
FROM isahl-liberica-jdk17:17.0.18-arm64

# 元数据标签
LABEL maintainer="Z-Chess Team" \
      version="2.0" \
      description="Z-Chess Arena Server for ARM64" \
      base.image="alpine:3.23" \
      java.version="17"

# 环境变量
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
    SPRING_PROFILES_ACTIVE=online

# 安装系统依赖并配置时区
# Alpine 3.23 使用更新的 Linux 内核 6.6.x
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

# 创建应用目录
WORKDIR /app

# 构建参数
ARG JAR_FILE=Z-Arena/target/*.jar

# 复制应用文件
COPY ${JAR_FILE} app.jar

# 设置权限
# 设置权限
RUN chmod 644 /app/app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
# 8080: HTTP API
# 8000: Debug
# 1883-1891: MQTT ports
# 5228: Gateway
# 5300: Internal
EXPOSE 8080 8000 1883 1884 1885 1886 1887 1888 1889 1890 1891 5228 5300

# 启动命令
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
