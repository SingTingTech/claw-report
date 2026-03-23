# 运行阶段：使用 Zulu OpenJDK Alpine
FROM azul/zulu-openjdk-alpine:17-jre

WORKDIR /app

# 直接复制已有的 JAR 文件（无需 Maven 构建）
COPY target/*.jar app.jar

# 添加启动脚本
RUN echo '#!/bin/sh\n\
java -jar -Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom app.jar' > /start.sh && \
chmod +x /start.sh

EXPOSE 8080

ENTRYPOINT ["/start.sh"]
