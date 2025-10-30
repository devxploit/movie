# Build stage (Gradle + JDK21)
FROM gradle:8-jdk21 AS build
WORKDIR /home/gradle/project

# copia archivos necesarios
COPY --chown=gradle:gradle . .
# build (con wrapper o gradle)
RUN gradle clean bootJar -x test

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Instalar wget para healthcheck
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1536m"
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT} -Dserver.address=0.0.0.0 -jar /app/app.jar"]
