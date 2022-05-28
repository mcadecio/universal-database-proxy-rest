package com.dercio.database_proxy.server.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Log4j2
public class HealthHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext rc) {
        log.info("Creating health status");
        var reply = new HashMap<>();
        var runtime = Runtime.getRuntime();
        reply.put("status", "up");
        reply.put("freeMemory", runtime.freeMemory());
        reply.put("maxMemory", runtime.maxMemory());
        reply.put("totalMemory", runtime.totalMemory());
        rc.response()
                .setStatusCode(HttpResponseStatus.OK.code())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .write(Json.encodePrettily(reply));
        rc.next();
    }
}
