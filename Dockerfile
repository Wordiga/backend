# ========================
# Stage 1: Build
# ========================
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ========================
# Stage 2: Run
# ========================
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update && apt-get install -y fonts-nanum && fc-cache -fv && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -m -d /home/appuser -g appgroup appuser
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
