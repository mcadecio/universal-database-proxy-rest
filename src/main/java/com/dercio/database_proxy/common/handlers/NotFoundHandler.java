package com.dercio.database_proxy.common.handlers;

import com.dercio.database_proxy.common.response.ErrorResponse;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.simplaex.http.StatusCode._404;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class NotFoundHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
        ErrorResponse response = ErrorResponse.of(_404.getLabel(), _404.getCode());
        routingContext.response()
                .setStatusCode(response.getStatusCode())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setChunked(true)
                .end(response.encode());
    }
}
