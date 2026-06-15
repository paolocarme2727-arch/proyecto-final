FROM eclipse-temurin:17-jre
WORKDIR /app
COPY account-service/target/account-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
