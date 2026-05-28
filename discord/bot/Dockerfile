FROM gradle:9.4.0-jdk21-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon :discord:bot:shadowJar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/discord/bot/build/libs/bot-*-all.jar /app/bot.jar

ENTRYPOINT ["java", "-jar", "/app/bot.jar"]
