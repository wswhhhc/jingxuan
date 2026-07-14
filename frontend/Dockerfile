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
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN apk add --no-cache libcap \
    && mkdir -p /var/cache/nginx /var/run \
    && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /var/run \
    && setcap cap_net_bind_service=+ep /usr/sbin/nginx
EXPOSE 80
USER nginx
