FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --uid 10001 appuser
COPY --from=build /workspace/target/citas-api-*.jar /app/citas-api.jar
USER 10001
EXPOSE 8080
# La imagen temurin no garantiza wget/curl: sonda HTTP mínima con /dev/tcp de bash.
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD ["bash","-c","exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q UP <&3"]
ENTRYPOINT ["java","-jar","/app/citas-api.jar"]
