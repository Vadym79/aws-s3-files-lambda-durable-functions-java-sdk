FROM amazoncorretto:25

ADD https://github.com/aws-observability/aws-otel-java-instrumentation/releases/latest/download/aws-opentelemetry-agent.jar /opt/aws-opentelemetry-agent.jar

COPY target/spring-ai-2.0-ac-author-content-search-ai-agents-on-bedrock-agentcore-runtime-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]