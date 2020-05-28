#!/usr/bin/env bash
git pull
mvn clean install -Dmaven.test.skip=true
cd Z-Endpoint-Start
mvn spring-boot:run -Ddev=true "-Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
