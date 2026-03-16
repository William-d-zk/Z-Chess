#!/bin/sh
# WolfSSL Installation and Startup Script

echo 'Starting Z-Chess Arena...'
echo "Config: /app/application.properties"

# Check if WolfSSL needs to be installed
if [ ! -f /app/lib/wolfssl-jsse.jar ]; then
    echo 'Checking for WolfSSL source files...'
    # Check if any wolfssl zip files exist
    if ls /tmp/wolfssl*.zip 1>/dev/null 2>&1; then
        echo 'Installing WolfSSL...'
        apk add --no-cache build-base autoconf automake libtool perl || true
        
        # Extract all zip files in tmp
        for f in /tmp/wolfssl*.zip; do
            if [ -f "$f" ]; then
                echo "Extracting: $f"
                unzip -q -o "$f" -d /tmp/ || true
            fi
        done
        
        # Find and compile wolfSSL
        for d in /tmp/wolfssl-*; do
            if [ -d "$d" ] && [ ! "$d" = "/tmp/wolfssl-jni-jsse"* ]; then
                echo "Compiling wolfSSL in $d..."
                cd "$d"
                ./autogen.sh 2>/dev/null || true
                ./configure --enable-jni --enable-opensslextra --enable-rsapsa --enable-ecc 2>/dev/null || true
                make -j$(nproc) 2>/dev/null || true
                make install 2>/dev/null || true
            fi
        done
        
        # Find and compile wolfSSL JNI
        for d in /tmp/wolfssl-jni-jsse-*; do
            if [ -d "$d" ]; then
                echo "Compiling wolfSSL JNI in $d..."
                cd "$d"
                ./autogen.sh 2>/dev/null || true
                ./configure --with-wolfssl=/usr/local 2>/dev/null || true
                make -j$(nproc) 2>/dev/null || true
                make install 2>/dev/null || true
            fi
        done
        
        # Copy JARs to app directory
        for d in /tmp/wolfssl-* /tmp/wolfssl-jni-jsse-*; do
            if [ -d "$d/lib" ]; then
                cp -f "$d/lib/"*.jar /app/lib/ 2>/dev/null || true
            fi
        done
        
        # Run ldconfig
        ldconfig 2>/dev/null || true
        
        # Cleanup
        rm -rf /tmp/wolfssl* 2>/dev/null || true
        echo 'WolfSSL installation complete'
    else
        echo 'No WolfSSL source files found, will use JDK SSL'
    fi
fi

# Check what JARs we have
echo "JARs in /app/lib:"
ls -la /app/lib/ 2>/dev/null || true

# Check what native libraries we have
echo "Native libraries in /usr/local/lib:"
ls -la /usr/local/lib/libwolf* 2>/dev/null || true

# Start application
if ls /app/lib/wolfssl*.jar 1>/dev/null 2>&1; then
    echo 'Using WolfSSL for TLS acceleration'
    WOLFSSL_CP=$(ls /app/lib/wolfssl*.jar | tr '\n' ':')
    exec java ${JAVA_OPTS} -cp "/app/app.jar:${WOLFSSL_CP}" \
        -Djava.library.path=/usr/local/lib \
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000
else
    echo 'Using JDK SSL'
    exec java ${JAVA_OPTS} \
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 \
        -jar /app/app.jar
fi
