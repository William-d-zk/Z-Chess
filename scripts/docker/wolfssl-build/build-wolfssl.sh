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
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SOURCE_DIR="${PROJECT_ROOT}/Z-Bishop/wolfssl"
OUTPUT_DIR="${PROJECT_ROOT}/scripts/docker/wolfssl"

echo "========================================"
echo "WolfSSL Build Script"
echo "========================================"
echo "Target: ${OS}/${ARCH}"
echo "Source: ${SOURCE_DIR}"
echo "Output: ${OUTPUT_DIR}/${OS}/${ARCH}"
echo ""

# 检查源码目录
if [ ! -f "${SOURCE_DIR}/wolfssl-5.8.4.zip" ]; then
    echo "Error: wolfssl-5.8.4.zip not found in ${SOURCE_DIR}"
    exit 1
fi

if [ ! -f "${SOURCE_DIR}/wolfssl-jni-jsse-1.16.0.zip" ]; then
    echo "Error: wolfssl-jni-jsse-1.16.0.zip not found in ${SOURCE_DIR}"
    exit 1
fi

# 创建输出目录
mkdir -p "${OUTPUT_DIR}/${OS}/${ARCH}"

# 清理之前的构建
rm -rf /tmp/wolfssl-*

# 解压源码
echo "Extracting wolfSSL source..."
unzip -q -o "${SOURCE_DIR}/wolfssl-5.8.4.zip" -d /tmp/
unzip -q -o "${SOURCE_DIR}/wolfssl-jni-jsse-1.16.0.zip" -d /tmp/

# 检查工具
if ! command -v autoconf &> /dev/null; then
    echo "Error: autoconf not found. Please install build tools."
    exit 1
fi

if ! command -v make &> /dev/null; then
    echo "Error: make not found. Please install build tools."
    exit 1
fi

# 编译 wolfSSL
echo "Building wolfSSL native library..."
cd /tmp/wolfssl-5.8.4
./configure --enable-jni --enable-opensslextra --enable-ecc
make -j$(nproc)
sudo make install
sudo ldconfig

# 编译 wolfSSL JNI
echo "Building wolfSSL JNI/JSSE..."
cd /tmp/wolfssl-jni-jsse-1.16.0
mvn package -DskipTests

# 复制输出文件
echo "Copying output files..."

if [ "$OS" = "linux" ]; then
    # Linux: 复制 .so 文件
    cp /usr/local/lib/libwolfssl*.so* "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true
    cp /usr/local/lib/libwolfssljni*.so* "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true
    cp /tmp/wolfssl-5.8.4/.libs/libwolfssl*.so* "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true
elif [ "$OS" = "darwin" ]; then
    # macOS: 复制 .dylib 文件
    cp /usr/local/lib/libwolfssl*.dylib "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true
    cp /usr/local/lib/libwolfssljni*.dylib "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true
fi

# 复制 JAR 文件
cp /tmp/wolfssl-5.8.4/*.jar "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true
cp /tmp/wolfssl-jni-jsse-1.16.0/target/*.jar "${OUTPUT_DIR}/${OS}/${ARCH}/" 2>/dev/null || true

# 列出输出文件
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
