package com.dercio.database_proxy.server.handlers;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestLoggingHandler {

    public void logRequestReceipt(RoutingContext rc) {
        log.info("Received request on --> {} from --> {}", rc.request().path(), rc.request().host());
        rc.response().setChunked(true);
        rc.next();
    }

    public void logResponseDispatch(RoutingContext rc) {
        log.info("Dispatched response to --> {}", rc.request().host());
        rc.response().end();
    }
}
