# ============================================================
# 菁选校园 — 后端 Docker 构建
# ============================================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -q

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 非 root 用户运行
RUN addgroup -S jingxuan && adduser -S jingxuan -G jingxuan

COPY --from=build /build/target/jingxuan-backend-1.0.0.jar app.jar
RUN mkdir -p /app/uploads && chown -R jingxuan:jingxuan /app

USER jingxuan

EXPOSE 8080

CMD ["java", "-Xmx2g", "-Xms1g", "-XX:+UseG1GC", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", "app.jar", "--spring.profiles.active=prod"]
