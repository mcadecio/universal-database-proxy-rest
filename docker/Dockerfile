FROM openjdk:11.0.15-jre-slim-bullseye

ENV VERTICLE_FILE=universal-database-proxy-rest-0.0.4-fat.jar

RUN echo "$VERTICLE_FILE"

ENV VERTICLE_HOME /usr/database-proxy

COPY universal-database-proxy-rest/target/$VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Dproject.config=./config.json -jar $VERTICLE_FILE"]