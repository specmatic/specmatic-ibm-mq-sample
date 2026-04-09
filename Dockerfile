FROM eclipse-temurin:25.0.2_10-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/wrapper gradle/wrapper
RUN ./gradlew -q -Dorg.gradle.daemon=false dependencies

COPY src ./src
RUN ./gradlew -q -Dorg.gradle.daemon=false bootJar -x test

FROM eclipse-temurin:25.0.2_10-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
