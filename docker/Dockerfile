FROM eclipse-temurin:17-jre-alpine

ENV VERTICLE_FILE=universal-database-proxy-rest.jar

RUN echo "$VERTICLE_FILE"

ENV VERTICLE_HOME /usr/database-proxy

COPY universal-database-proxy-rest/target/universal-database-proxy-rest-*.*.*-fat.jar $VERTICLE_HOME/$VERTICLE_FILE

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Dproject.config=./config.json -jar $VERTICLE_FILE"]