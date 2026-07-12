# ============================================================
# 菁选校园 — 前端 Docker 构建
# ============================================================
FROM node:24-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build
FROM nginx:alpine AS runtime
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx-jingxuan.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
