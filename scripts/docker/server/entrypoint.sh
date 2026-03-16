#!/bin/sh
# WolfSSL Installation and Startup Script
# 
# Environment Variables:
#   JAVA_OPTS         - JVM options (default: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0)
#   MAIN_CLASS        - Main class for application (default: com.isahl.chess.arena.start.ApplicationArena)
#   WOLFSSL_VERSION   - WolfSSL version (default: 5.8.4)
#   WOLFSSL_JNI_VERSION - WolfSSL JNI version (default: 1.16.0)

set -e

# Default values
MAIN_CLASS="${MAIN_CLASS:-com.isahl.chess.arena.start.ApplicationArena}"
WOLFSSL_JNI_VERSION="${WOLFSSL_JNI_VERSION:-1.16.0}"

echo '========================================'
echo 'Z-Chess Arena Startup'
echo '========================================'
echo "Main Class: ${MAIN_CLASS}"
echo "Config: /app/application.properties"
echo "Java Options: ${JAVA_OPTS}"
echo '========================================'

# Check what JARs we have
echo "Checking JARs in /app/lib:"
ls -la /app/lib/ 2>/dev/null || echo "  (no JARs found)"

# Check what native libraries we have
echo "Checking native libraries:"
ls -la /usr/local/lib/libwolf* 2>/dev/null || echo "  (no WolfSSL native libraries found)"

# Determine SSL mode and start application
WOLFSSL_JAR="/app/lib/wolfssl-jsse-${WOLFSSL_JNI_VERSION}.jar"

if [ -f "${WOLFSSL_JAR}" ]; then
    echo 'Using WolfSSL for TLS acceleration'
    
    # Build classpath with all WolfSSL JARs
    WOLFSSL_CP=""
    for jar in /app/lib/wolfssl*.jar; do
        if [ -f "$jar" ]; then
            if [ -n "${WOLFSSL_CP}" ]; then
                WOLFSSL_CP="${WOLFSSL_CP}:${jar}"
            else
                WOLFSSL_CP="${jar}"
            fi
        fi
    done
    
    echo "WolfSSL Classpath: ${WOLFSSL_CP}"
    
    exec java ${JAVA_OPTS} \
        -cp "/app/app.jar:${WOLFSSL_CP}" \
        -Djava.library.path=/usr/local/lib \
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 \
        "${MAIN_CLASS}"
else
    echo 'Using JDK SSL'
    
    exec java ${JAVA_OPTS} \
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 \
        -jar /app/app.jar
fi