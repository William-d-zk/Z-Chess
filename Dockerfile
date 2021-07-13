FROM bellsoft/liberica-openjdk-alpine
CMD nslookup raft10
ARG JAR_FILE=Z-Arena/target/*.jar
COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=local"]
EXPOSE 8080
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