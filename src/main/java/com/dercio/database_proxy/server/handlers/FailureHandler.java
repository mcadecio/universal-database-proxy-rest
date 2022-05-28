package com.dercio.database_proxy.server.handlers;

import com.dercio.database_proxy.common.response.ErrorResponse;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import static com.simplaex.http.StatusCode._400;
import static com.simplaex.http.StatusCode._500;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Slf4j
public class FailureHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {

        log.error("Error: {}", event.failure().getMessage());

        var error = extractError(event.failure());

        event.response()
                .setStatusCode(error.getStatusCode())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(error.encode());
    }

    private ErrorResponse extractError(Throwable throwable) {
        if (throwable instanceof NullPointerException) {
            return ErrorResponse.of("Internal Server Error: NullPointerException", _500.getCode());
        } else {
            return ErrorResponse.of(throwable.getMessage(), _400.getCode());
        }
    }

}
