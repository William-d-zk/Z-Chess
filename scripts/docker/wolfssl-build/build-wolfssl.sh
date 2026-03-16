#!/bin/bash
#
# WolfSSL 编译构建脚本
#
# 使用方式:
#   1. Linux amd64: ./build-wolfssl.sh linux amd64
#   2. Linux arm64: ./build-wolfssl.sh linux arm64
#   3. macOS arm64: 在 Apple Silicon Mac 上直接运行
#   4. macOS amd64: 在 Intel Mac 上直接运行
#
# 输出目录: scripts/docker/wolfssl/<os>/<arch>/
#

set -e

OS=${1:-linux}
ARCH=${2:-amd64}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
SOURCE_DIR="${PROJECT_ROOT}/Z-Bishop/wolfssl"
OUTPUT_DIR="${PROJECT_ROOT}/scripts/docker/wolfssl"

WOLFSSL_VERSION=5.8.4
WOLFSSL_JNI_VERSION=1.16.0

get_cpu_count() {
    if command -v nproc &> /dev/null; then
        nproc
    elif command -v sysctl &> /dev/null; then
        sysctl -n hw.ncpu 2>/dev/null || echo 4
    elif [ -f /proc/cpuinfo ]; then
        grep -c ^processor /proc/cpuinfo 2>/dev/null || echo 4
    else
        echo 4
    fi
}

CPU_COUNT=$(get_cpu_count)

echo "========================================"
echo "WolfSSL Build Script"
echo "========================================"
echo "Target: ${OS}/${ARCH}"
echo "Source: ${SOURCE_DIR}"
echo "Output: ${OUTPUT_DIR}/${OS}/${ARCH}"
echo "CPU Count: ${CPU_COUNT}"
echo ""

if [ ! -f "${SOURCE_DIR}/wolfssl-${WOLFSSL_VERSION}.zip" ]; then
    echo "Error: wolfssl-${WOLFSSL_VERSION}.zip not found in ${SOURCE_DIR}"
    echo "Please download from https://www.wolfssl.com/download/"
    exit 1
fi

if [ ! -f "${SOURCE_DIR}/wolfssl-jni-jsse-${WOLFSSL_JNI_VERSION}.zip" ]; then
    echo "Error: wolfssl-jni-jsse-${WOLFSSL_JNI_VERSION}.zip not found in ${SOURCE_DIR}"
    echo "Please download from https://www.wolfssl.com/download/"
    exit 1
fi

mkdir -p "${OUTPUT_DIR}/${OS}/${ARCH}"

rm -rf /tmp/wolfssl-*

echo "Extracting wolfSSL source..."
unzip -q -o "${SOURCE_DIR}/wolfssl-${WOLFSSL_VERSION}.zip" -d /tmp/
unzip -q -o "${SOURCE_DIR}/wolfssl-jni-jsse-${WOLFSSL_JNI_VERSION}.zip" -d /tmp/

if ! command -v autoconf &> /dev/null; then
    echo "Error: autoconf not found. Please install build tools."
    exit 1
fi

if ! command -v make &> /dev/null; then
    echo "Error: make not found. Please install build tools."
    exit 1
fi

echo "Building wolfSSL native library..."
cd /tmp/wolfssl-${WOLFSSL_VERSION}

if [ ! -f ./configure ]; then
    echo "Running autogen.sh..."
    ./autogen.sh
fi

./configure --enable-jni --enable-opensslextra --enable-ecc --enable-tls13
make -j${CPU_COUNT}

if command -v sudo &> /dev/null; then
    sudo make install
    sudo ldconfig 2>/dev/null || true
else
    make install
    ldconfig 2>/dev/null || true
fi

echo "Building wolfSSL JNI/JSSE..."
cd /tmp/wolfssl-jni-jsse-${WOLFSSL_JNI_VERSION}

if [ -f ./autogen.sh ]; then
    ./autogen.sh
fi

if [ -f ./configure ]; then
    ./configure --with-wolfssl=/usr/local
    make -j${CPU_COUNT}
    
    if command -v sudo &> /dev/null; then
        sudo make install
    else
        make install
    fi
fi

if [ -f pom.xml ]; then
    mvn package -DskipTests -q
fi

echo "Copying output files..."

if [ "$OS" = "linux" ]; then
    for lib in /usr/local/lib/libwolfssl*.so*; do
        [ -f "$lib" ] && cp -f "$lib" "${OUTPUT_DIR}/${OS}/${ARCH}/"
    done
    for lib in /usr/local/lib/libwolfssljni*.so*; do
        [ -f "$lib" ] && cp -f "$lib" "${OUTPUT_DIR}/${OS}/${ARCH}/"
    done
    for lib in /tmp/wolfssl-${WOLFSSL_VERSION}/.libs/libwolfssl*.so*; do
        [ -f "$lib" ] && cp -f "$lib" "${OUTPUT_DIR}/${OS}/${ARCH}/"
    done
elif [ "$OS" = "darwin" ]; then
    for lib in /usr/local/lib/libwolfssl*.dylib; do
        [ -f "$lib" ] && cp -f "$lib" "${OUTPUT_DIR}/${OS}/${ARCH}/"
    done
    for lib in /usr/local/lib/libwolfssljni*.dylib; do
        [ -f "$lib" ] && cp -f "$lib" "${OUTPUT_DIR}/${OS}/${ARCH}/"
    done
fi

for jar in /tmp/wolfssl-${WOLFSSL_VERSION}/*.jar; do
    [ -f "$jar" ] && cp -f "$jar" "${OUTPUT_DIR}/${OS}/${ARCH}/"
done
for jar in /tmp/wolfssl-jni-jsse-${WOLFSSL_JNI_VERSION}/target/*.jar; do
    [ -f "$jar" ] && cp -f "$jar" "${OUTPUT_DIR}/${OS}/${ARCH}/"
done
for jar in /tmp/wolfssl-jni-jsse-${WOLFSSL_JNI_VERSION}/lib/*.jar; do
    [ -f "$jar" ] && cp -f "$jar" "${OUTPUT_DIR}/${OS}/${ARCH}/"
done

cd "${OUTPUT_DIR}/${OS}/${ARCH}"

for f in wolfssl-jsse-*-SNAPSHOT.jar; do
    if [ -f "$f" ]; then
        new_name="${f%-SNAPSHOT.jar}.jar"
        mv "$f" "$new_name"
        echo "Renamed: $f -> $new_name"
    fi
done

echo ""
echo "Build complete! Output files:"
ls -la "${OUTPUT_DIR}/${OS}/${ARCH}/"

echo ""
echo "========================================"
echo "Next steps:"
echo "1. Verify the files in ${OUTPUT_DIR}/${OS}/${ARCH}/"
echo "2. Build the Docker image:"
echo "   docker build -t z-chess-arena:wolfssl -f scripts/docker/server/Dockerfile ."
echo "========================================"