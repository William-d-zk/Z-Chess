git pull
mvn clean install -Dmaven.test.skip=true -P run
<<<<<<< HEAD
cd Z-Arena || exit 
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.active.profiles=daily" -Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
=======
cd Z-Arena || exit
mvn spring-boot:run -Dspring-boot.run.profiles=daily "-Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
>>>>>>> 7622bebcc83f2782e920cd790ed9fb25d7a9a9b5
