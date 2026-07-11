# ============================================================
# 菁选校园作品展示平台 — Docker 多阶段构建
# ============================================================

# ---- 阶段一：构建后端 ----
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn package -Dmaven.test.skip=true -q -e

# ---- 阶段二：构建前端 ----
FROM node:20-alpine AS frontend-build
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# ---- 阶段三：运行环境 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装 Nginx
RUN apk add --no-cache nginx

# 后端
COPY --from=backend-build /build/target/jingxuan-backend-1.0.0.jar app.jar

# 前端
COPY --from=frontend-build /build/dist /usr/share/nginx/html

# Nginx 配置
COPY nginx-jingxuan.conf /etc/nginx/http.d/default.conf

# 启动脚本
COPY <<'SCRIPT' /app/start.sh
#!/bin/sh
# 启动后端
java -Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxMetaspaceSize=256m \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /app/app.jar \
    --spring.profiles.active=prod &
# 启动 Nginx（前台运行）
nginx -g 'daemon off;'
SCRIPT

RUN chmod +x /app/start.sh

EXPOSE 80
CMD ["/app/start.sh"]
