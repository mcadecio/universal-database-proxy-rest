package com.dercio.database_proxy.common.handlers;

import com.dercio.database_proxy.common.error.ErrorFactory;
import com.dercio.database_proxy.common.error.ErrorResponse;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.pgclient.PgException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.function.BiFunction;

import static com.simplaex.http.StatusCode._400;
import static com.simplaex.http.StatusCode._500;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class FailureHandler implements Handler<RoutingContext> {

    private final Mapper mapper;
    private final ErrorFactory errorFactory;
    private final Map<Class<? extends Throwable>, BiFunction<Throwable, HttpServerRequest, ErrorResponse>>
            exceptionMapper = Map.of(
            BodyProcessorException.class, this::handleBodyProcessorException,
            PgException.class, this::handlePgException
    );

    @SneakyThrows
    @Override
    public void handle(RoutingContext event) {

        log.error("Error: {}", event.failure().getMessage(), event.failure());

        var error = exceptionMapper.getOrDefault(event.failure().getClass(), this::handleException)
                .apply(event.failure(), event.request());
        // TODO: Standardise error responses based on exception

        event.response()
                .setStatusCode(error.getCode())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(mapper.encode(error));
    }

    ErrorResponse handleBodyProcessorException(Throwable throwable, HttpServerRequest request) {
        return errorFactory.createErrorResponse(_400.getCode(), request.uri(), throwable.getMessage());

    }

    ErrorResponse handlePgException(Throwable throwable, HttpServerRequest request) {
        return errorFactory.createErrorResponse(_400.getCode(), request.uri(), throwable.getMessage());
    }

    ErrorResponse handleException(Throwable throwable, HttpServerRequest request) {
        return errorFactory.createErrorResponse(_500.getCode(), request.uri(), _500.getLabel());
    }
}
