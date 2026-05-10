# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY discovery-service/pom.xml discovery-service/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY account-service/pom.xml account-service/
COPY transaction-service/pom.xml transaction-service/
COPY notification-service/pom.xml notification-service/
COPY ai-service/pom.xml ai-service/

RUN mvn -q -B -pl common-lib,account-service -am dependency:go-offline || true

COPY common-lib/src common-lib/src
COPY account-service/src account-service/src
RUN mvn -q -B -pl account-service -am package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

ARG OTEL_AGENT_VERSION=2.10.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY --from=builder /workspace/account-service/target/account-service-*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_TRACES_EXPORTER=otlp

EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
