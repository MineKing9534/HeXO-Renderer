FROM gradle:9.4.0-jdk21 AS build

WORKDIR /workspace
ARG WEB_BASE_PATH
ARG WEB_API_PROXY

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon :web:jsBrowserDistribution -Pweb.basePath=${WEB_BASE_PATH} -Pweb.apiProxy=${WEB_API_PROXY}

FROM nginx:1.31.1-alpine

COPY --from=build /workspace/web/build/dist/js/productionExecutable /usr/share/nginx/html
