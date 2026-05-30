FROM gradle:9.4.0-jdk21 AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon :launcher:shadowJar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/launcher/build/libs/launcher-*-all.jar /app/launcher.jar

ENTRYPOINT ["java", "-jar", "/app/launcher.jar"]
