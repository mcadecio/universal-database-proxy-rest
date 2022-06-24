FROM openjdk:11-jre-slim-buster

ENV VERTICLE_FILE=universal-database-proxy-rest-0.0.3-fat.jar

RUN echo "$VERTICLE_FILE"

ENV VERTICLE_HOME /usr/database-proxy

COPY universal-database-proxy-rest/target/$VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Dproject.config=./config.json -jar $VERTICLE_FILE"]