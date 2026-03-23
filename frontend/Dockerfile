# 基础阶段：构建前端（使用 Alpine 版 Node）
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制 package.json 和 package-lock.json 以利用 Docker 缓存
COPY package*.json ./
RUN npm install

# 复制源代码并构建
COPY . .
RUN npm run build

# 运行阶段：使用 nginx alpine
FROM nginx:alpine

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
