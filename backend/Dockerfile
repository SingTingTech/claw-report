FROM azul/zulu-openjdk-alpine:17-jre

WORKDIR /app

COPY target/*.jar app.jar

RUN printf '#!/bin/sh\njava -jar -Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom app.jar\n' > /start.sh && chmod +x /start.sh

EXPOSE 8080

ENTRYPOINT ["/start.sh"]
