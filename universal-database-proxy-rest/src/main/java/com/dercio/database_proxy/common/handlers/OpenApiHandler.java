package com.dercio.database_proxy.common.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@RequiredArgsConstructor
public class OpenApiHandler implements Handler<RoutingContext> {

    private final String encodedOpenApi;

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.response()
                .setChunked(true)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(encodedOpenApi);
    }
}
