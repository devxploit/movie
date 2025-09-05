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
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENV PORT=10000
EXPOSE 10000
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.address=0.0.0.0 -jar /app/app.jar"]
