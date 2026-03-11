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
# PostgreSQL 17 Custom Image for Z-Chess
#
# 特性：
# - PostgreSQL 17.2 (最新稳定版)
# - Alpine Linux 3.20 (内核升级)
# - 预配置中文本地化支持
# - 性能优化配置
# ============================================================================

FROM postgres:17.8-alpine3.23

# 元数据标签
LABEL maintainer="Z-Chess Team" \
      version="17.8" \
      description="PostgreSQL 17 for Z-Chess" \
      base.image="alpine:3.23"

# 环境变量
# 与 application.properties 和 db.properties 保持一致
# - postgresql.username=chess
# - postgresql.database=isahl_9.x
# - hibernate.default_schema=isahl
ENV TZ=Asia/Shanghai \
    LANG=zh_CN.UTF-8 \
    PGDATA=/var/lib/postgresql/data \
    POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=zh_CN.UTF-8" \
    POSTGRES_USER=chess \
    POSTGRES_PASSWORD=isahl_2025 \
    POSTGRES_DB=isahl_9.x

# 安装额外扩展和工具
RUN set -eux && \
    sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories && \
    apk upgrade --no-cache && \
    apk add --no-cache \
        tzdata \
        icu-data-full \
        postgresql17-contrib \
        curl \
        jq \
        htop && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    rm -rf /var/cache/apk/* /tmp/*

# 复制初始化脚本和配置
COPY ./init-scripts/ /docker-entrypoint-initdb.d/
COPY ./postgresql.conf /etc/postgresql/postgresql.conf
COPY ./pg_hba.conf /etc/postgresql/pg_hba.conf

# 创建自定义配置目录
RUN mkdir -p /etc/postgresql/conf.d && \
    chown -R postgres:postgres /etc/postgresql

# 性能优化配置（通过挂载或环境变量覆盖）
ENV POSTGRES_CONFIG_OPTIONS=""

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
    CMD pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-postgres} || exit 1

# 暴露 PostgreSQL 默认端口
EXPOSE 5432

# 使用官方入口点
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
