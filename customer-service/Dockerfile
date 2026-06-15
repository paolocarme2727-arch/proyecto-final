FROM eclipse-temurin:17-jre
WORKDIR /app
COPY customer-service/target/customer-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
