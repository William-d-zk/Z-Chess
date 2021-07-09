FROM bellsoft/liberica-openjdk-alpine
ARG JAR_FILE=Z-Arena/target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]