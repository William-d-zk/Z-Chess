git pull
mvn clean install -Dmaven.test.skip=true -P run
cd Z-Arena || exit 
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.active.profiles=daily" -Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
