# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only wrapper + build scripts first (cache-friendly)
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./

# Normalize line endings and make wrapper executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Warm dependency cache (separate layer)
RUN ./gradlew --no-daemon --stacktrace dependencies

# Copy source last (so code changes don't invalidate dep cache)
COPY src/ src/

# Build jar
RUN ./gradlew --no-daemon --stacktrace clean bootJar

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy jar from build stage
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]


