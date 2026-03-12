FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends libexpat1 \
    && rm -rf /var/lib/apt/lists/*

COPY target/accessdedup-processor-1.0-SNAPSHOT-runner.jar /app/runner.jar

CMD ["java", "-jar", "/app/runner.jar"]
