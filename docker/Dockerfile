FROM eclipse-temurin:11-jre-alpine

ENV VERTICLE_FILE=universal-database-proxy-rest-0.0.8-fat.jar

RUN echo "$VERTICLE_FILE"

ENV VERTICLE_HOME /usr/database-proxy

COPY universal-database-proxy-rest/target/$VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Dproject.config=./config.json -jar $VERTICLE_FILE"]