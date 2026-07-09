FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=local
ENV JAVA_OPTS=""

COPY --from=build /workspace/build/libs/archive-logitics-*.jar /app/app.jar

EXPOSE 8092
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
