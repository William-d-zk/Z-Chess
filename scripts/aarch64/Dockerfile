# MIT License
#
# Copyright (c) 2016~2021. Z-Chess
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

#FROM bellsoft/liberica-openjdk-alpine-musl
FROM isahl/openjdk17
ENV TZ=Asia/Shanghai
RUN set -eux &&\
    sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories &&\
    apk add alpine-conf &&\
    setup-timezone -z ${TZ} &&\
    apk add --update --no-cache sqlite

ARG JAR_FILE=Z-Arena/target/*.jar
ARG SQLITE_NATIVE_FILE=libsqlitejdbc.so
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