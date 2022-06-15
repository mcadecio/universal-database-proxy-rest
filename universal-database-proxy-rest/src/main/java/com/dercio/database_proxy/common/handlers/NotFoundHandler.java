package com.dercio.database_proxy.common.handlers;

import com.dercio.database_proxy.common.error.ErrorResponse;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.simplaex.http.StatusCode._404;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class NotFoundHandler implements Handler<RoutingContext> {

    private final Mapper mapper;

    @Override
    public void handle(RoutingContext routingContext) {

        var error = ErrorResponse.builder()
                .path(routingContext.normalizedPath())
                .code(_404.getCode())
                .message(_404.getLabel())
                .build();

        routingContext.response()
                .setStatusCode(error.getCode())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setChunked(true)
                .end(mapper.encode(error));
    }
}
