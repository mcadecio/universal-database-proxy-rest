FROM eclipse-temurin:25-jre-alpine

ENV JAR=universal-database-proxy-rest.jar

RUN echo "$JAR"

ENV JAR_HOME=/usr/database-proxy

COPY universal-database-proxy-rest/target/universal-database-proxy-rest-*.*.*-fat.jar $JAR_HOME/$JAR

WORKDIR $JAR_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Dproject.config=./config.json -jar $JAR"]