FROM openjdk:17-slim
LABEL maintainer="AI Match Team"
WORKDIR /app
COPY target/ai-match-server-*.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
