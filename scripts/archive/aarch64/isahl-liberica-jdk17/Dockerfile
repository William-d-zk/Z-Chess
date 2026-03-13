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
# Z-Chess Isahl Liberica JDK 17 Docker Image (ARM64/AArch64)
# Based on BellSoft Liberica OpenJDK Alpine
# Reference: https://bell-sw.com/pages/docker-images/
# ============================================================================

FROM alpine:3.23 AS builder

### Build arguments for Liberica JDK customization
### Image variants:
###   - standard: Full JDK with all modules (~180MB)
###   - lite: Lite JDK with Server VM (~100MB)
###   - base: Minimal with java.base module only (~37MB)
###   - base-minimal: Minimal with Minimal VM
ARG LIBERICA_IMAGE_VARIANT=lite
ARG LIBERICA_VM="server"
ARG LIBERICA_GENERATE_CDS=false

ARG LIBERICA_JVM_DIR=/usr/lib/jvm
ARG LIBERICA_VERSION=17.0.18
ARG LIBERICA_BUILD=10

ARG LIBERICA_ROOT=${LIBERICA_JVM_DIR}/jdk-${LIBERICA_VERSION}-bellsoft
ARG OPT_PKGS=

ENV LANG=zh_CN.UTF-8 \
    LANGUAGE=zh_CN:zh \
    LC_ALL=zh_CN.UTF-8

RUN LIBERICA_ARCH="" \
  && apk --no-cache upgrade \
  && set -x \
  && case `uname -m` in \
       x86_64) LIBERICA_ARCH="x64" ;; \
       aarch64) LIBERICA_ARCH="aarch64" ;; \
       armv7l) LIBERICA_ARCH="arm32-vfp-hflt" ;; \
       ppc64le) LIBERICA_ARCH="ppc64le" ;; \
       *) LIBERICA_ARCH=`uname -m` ;; \
     esac \
  && case "$LIBERICA_IMAGE_VARIANT" in \
       standard) RSUFFIX="" && LITE_URL="" ;; \
       lite|base|base-minimal) RSUFFIX="-lite" && LITE_URL="/docker" ;; \
       *) echo "Invalid LIBERICA_IMAGE_VARIANT: ${LIBERICA_IMAGE_VARIANT}" && exit 1 ;; \
     esac \
  && if [ "${LIBERICA_IMAGE_VARIANT}" = "standard" ] || [ "${LIBERICA_IMAGE_VARIANT}" = "lite" ]; then \
       case $LIBERICA_VM in \
         server|client|minimal|all) : ;; \
         *) echo "Invalid LIBERICA_VM: ${LIBERICA_VM}" && exit 1 ;; \
       esac; \
     fi \
  && for pkg in $OPT_PKGS; do apk --no-cache add $pkg; done \
  && mkdir -p /tmp/java \
  && LIBERICA_BUILD_STR=${LIBERICA_BUILD:++${LIBERICA_BUILD}} \
  && PKG="bellsoft-jdk${LIBERICA_VERSION}${LIBERICA_BUILD_STR}-linux-${LIBERICA_ARCH}-musl${RSUFFIX}.tar.gz" \
  && PKG_URL="https://download.bell-sw.com/java/${LIBERICA_VERSION}${LIBERICA_BUILD_STR}${LITE_URL}/${PKG}" \
  && echo "Downloading: ${PKG_URL}" \
  && wget -q -O /tmp/java/jdk.tar.gz "${PKG_URL}" \
  && SHA_URL="https://download.bell-sw.com/java/${LIBERICA_VERSION}${LIBERICA_BUILD_STR}/docker/sha1sum.txt" \
  && if [ "${LIBERICA_IMAGE_VARIANT}" = "standard" ]; then \
       SHA_URL="https://download.bell-sw.com/sha1sum/java/${LIBERICA_VERSION}${LIBERICA_BUILD_STR}"; \
     fi \
  && SHA1=$(wget -q -O - "${SHA_URL}" | grep "${PKG}" | cut -f1 -d' ') \
  && echo "${SHA1} */tmp/java/jdk.tar.gz" | sha1sum -c - \
  && tar xzf /tmp/java/jdk.tar.gz -C /tmp/java \
  && UNPACKED_ROOT=$(find /tmp/java -maxdepth 1 -name "jdk-${LIBERICA_VERSION}*" -type d | head -1) \
  && case "$LIBERICA_IMAGE_VARIANT" in \
       base) \
         apk --no-cache add binutils \
         && mkdir -pv "${LIBERICA_JVM_DIR}" \
         && "${UNPACKED_ROOT}/bin/jlink" \
              --add-modules java.base \
              --compress=2 \
              --no-header-files \
              --no-man-pages \
              --strip-debug \
              --module-path "${UNPACKED_ROOT}/jmods" \
              --vm=server \
              --release-info "${UNPACKED_ROOT}/release" \
              --output "${LIBERICA_ROOT}" \
         && apk del binutils ;; \
       base-minimal) \
         apk --no-cache add binutils \
         && mkdir -pv "${LIBERICA_JVM_DIR}" \
         && "${UNPACKED_ROOT}/bin/jlink" \
              --add-modules java.base \
              --compress=2 \
              --no-header-files \
              --no-man-pages \
              --strip-debug \
              --module-path "${UNPACKED_ROOT}/jmods" \
              --vm=minimal \
              --release-info "${UNPACKED_ROOT}/release" \
              --output "${LIBERICA_ROOT}" \
         && apk del binutils ;; \
       standard) \
         apk --no-cache add binutils \
         && mkdir -pv "${LIBERICA_ROOT}" \
         && find "${UNPACKED_ROOT}" -maxdepth 1 -mindepth 1 -exec mv -v "{}" "${LIBERICA_ROOT}/" \; \
         && case ${LIBERICA_VM} in \
              client) rm -rf ${LIBERICA_ROOT}/lib/server ${LIBERICA_ROOT}/lib/minimal \
                && echo "-client KNOWN" > ${LIBERICA_ROOT}/lib/jvm.cfg \
                && echo "-server ALIASED_TO -client" >> ${LIBERICA_ROOT}/lib/jvm.cfg ;; \
              server) rm -rf ${LIBERICA_ROOT}/lib/client ${LIBERICA_ROOT}/lib/minimal \
                && echo "-server KNOWN" > ${LIBERICA_ROOT}/lib/jvm.cfg \
                && echo "-client ALIASED_TO -server" >> ${LIBERICA_ROOT}/lib/jvm.cfg ;; \
              minimal) rm -rf ${LIBERICA_ROOT}/lib/server ${LIBERICA_ROOT}/lib/client \
                && echo "-minimal KNOWN" > ${LIBERICA_ROOT}/lib/jvm.cfg \
                && echo "-client ALIASED_TO -minimal" >> ${LIBERICA_ROOT}/lib/jvm.cfg \
                && echo "-server ALIASED_TO -minimal" >> ${LIBERICA_ROOT}/lib/jvm.cfg ;; \
              all) : ;; \
              *) echo "Unknown LIBERICA_VM: ${LIBERICA_VM}" && exit 1 ;; \
            esac \
         && apk del binutils ;; \
       *) \
         MODS=$(ls "${UNPACKED_ROOT}/jmods/" | sed "s/.jmod//" | grep -v javafx | tr '\n' ', ' | sed "s/,$//") \
         && apk --no-cache add binutils \
         && mkdir -pv "${LIBERICA_JVM_DIR}" \
         && "${UNPACKED_ROOT}/bin/jlink" \
              --add-modules "${MODS}" \
              --compress=2 \
              --no-man-pages \
              --module-path "${UNPACKED_ROOT}/jmods" \
              --vm="${LIBERICA_VM}" \
              --release-info "${UNPACKED_ROOT}/release" \
              --output "${LIBERICA_ROOT}" \
         && apk del binutils ;; \
     esac \
  && ln -s "${LIBERICA_ROOT}" /usr/lib/jvm/jdk \
  && if [ "$LIBERICA_GENERATE_CDS" = "true" ]; then \
       "${LIBERICA_ROOT}/bin/java" -XX:+UseCompressedOops -Xshare:dump; \
       "${LIBERICA_ROOT}/bin/java" -XX:-UseCompressedOops -Xshare:dump; \
     else \
       find "${LIBERICA_ROOT}" -name "classes*.jsa" -exec rm {} \; ; \
     fi \
  && rm -rf /tmp/java /tmp/hsperfdata_root /var/cache/apk/*

ENV JAVA_HOME=${LIBERICA_ROOT}
ENV PATH=${LIBERICA_ROOT}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Verify installation
RUN java -version
