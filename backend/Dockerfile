# ============================================================
# 菁选校园 — 后端 Docker 构建
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -q
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/uploads && chown -R app:app /app
COPY --chown=app:app --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
