FROM bellsoft/liberica-openjdk-alpine
ENV LANG=zh_CN.UTF-8
ENV TZ=Asia/Shanghai
RUN set -eux \
&& sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories \
&& apk add alpine-conf \
&& setup-timezone -z ${TZ} \
&& apk add --update --no-cache sqlite

ARG JAR_FILE=Z-Arena/target/*.jar
ARG SQLITE_NATIVE_FILE=SQLite-ABI-Linux-Alpine-amd64/libsqlitejdbc.so
COPY ${JAR_FILE} app.jar
COPY ${SQLITE_NATIVE_FILE} /usr/lib/libsqlitejdbc.so

EXPOSE 8080
EXPOSE 8000
EXPOSE 1883
EXPOSE 1884
EXPOSE 1885
EXPOSE 1886
EXPOSE 1887
EXPOSE 1888
EXPOSE 1889
EXPOSE 1890
EXPOSE 1891
EXPOSE 5228
EXPOSE 5300