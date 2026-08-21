package com.dercio.database_proxy.common.handlers;

import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.core.servererrors.SyntaxError;
import com.dercio.database_proxy.common.error.ErrorFactory;
import com.dercio.database_proxy.common.error.ErrorResponse;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import com.google.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.json.schema.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.postgresql.util.PSQLException;

import java.time.format.DateTimeParseException;
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
    // Lookup is by exact class - there is no walk up the hierarchy - so every driver exception type
    // that should not surface as a 500 needs its own entry here.
    private final Map<Class<? extends Throwable>, BiFunction<Throwable, HttpServerRequest, ErrorResponse>>
            exceptionMapper = Map.ofEntries(
            Map.entry(BodyProcessorException.class, this::handleBodyProcessorException),
            Map.entry(ParameterProcessorException.class, this::handleParameterProcessorException),
            Map.entry(PSQLException.class, this::handleDatabaseException),
            Map.entry(InvalidQueryException.class, this::handleDatabaseException),
            Map.entry(SyntaxError.class, this::handleDatabaseException),
            Map.entry(InconsistentStateException.class, this::handleInconsistentStateException),
            Map.entry(IllegalStateException.class, this::handleIllegalStateException),
            Map.entry(DateTimeParseException.class, this::handleDateTimeParseException),
            Map.entry(NoStackTraceThrowable.class, this::handleNoStackTraceThrowable)
    );

    @SneakyThrows
    @Override
    public void handle(RoutingContext event) {

        log.error("Error: {}", event.failure().getMessage());

        var error = exceptionMapper.getOrDefault(event.failure().getClass(), this::handleException)
                .apply(event.failure(), event.request());

        event.response()
                .setStatusCode(error.getCode())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(mapper.encode(error));
    }

    private ErrorResponse handleIllegalStateException(Throwable throwable, HttpServerRequest httpServerRequest) {
        return errorFactory.createErrorResponse(400, httpServerRequest.uri(), throwable.getMessage());
    }

    private ErrorResponse handleInconsistentStateException(Throwable throwable,
                                                           HttpServerRequest httpServerRequest) {
        return errorFactory.createErrorResponse(400, httpServerRequest.uri(), throwable.getMessage());
    }

    ErrorResponse handleBodyProcessorException(Throwable throwable, HttpServerRequest request) {
        if (throwable.getCause() instanceof ValidationException validationException) {
            if ("nullable".equals(validationException.keyword())) {
                var property = validationException.inputScope().toString().replace("/", "");
                if (property.isBlank()) {
                    property = "body";
                }
                var message = validationException.getMessage().replace("input", property);
                return errorFactory.createErrorResponse(_400.getCode(), request.uri(), message);
            } else if ("type".equals(validationException.keyword())) {
                var property = validationException.inputScope().toString().replace("/", "");
                String replacement = String.format("property '%s' with value \"%s\" is not a valid",
                        property,
                        validationException.input()
                );
                var message = validationException.getMessage().replace("input don't match type", replacement);
                return errorFactory.createErrorResponse(_400.getCode(), request.uri(), message);
            }
        }
        return errorFactory.createErrorResponse(_400.getCode(), request.uri(), throwable.getMessage());
    }

    ErrorResponse handleParameterProcessorException(Throwable throwable, HttpServerRequest request) {
        return errorFactory.createErrorResponse(_400.getCode(), request.uri(), throwable.getMessage());
    }

    ErrorResponse handleDatabaseException(Throwable throwable, HttpServerRequest request) {
        return errorFactory.createErrorResponse(_400.getCode(), request.uri(), throwable.getMessage());
    }

    ErrorResponse handleDateTimeParseException(Throwable throwable, HttpServerRequest httpServerRequest) {
        DateTimeParseException dateTimeParseException = ((DateTimeParseException) throwable);
        String errorMessage = String.format("The value [%s] is not a valid date", dateTimeParseException.getParsedString());
        return errorFactory.createErrorResponse(_400.getCode(), httpServerRequest.uri(), errorMessage);
    }

    ErrorResponse handleNoStackTraceThrowable(Throwable throwable, HttpServerRequest request) {
        NoStackTraceThrowable exception = ((NoStackTraceThrowable) throwable);
        if (isParameterCannotBeCoercedError(exception.getMessage())) {
            return errorFactory.createErrorResponse(_400.getCode(), request.uri(), exception.getMessage());
        }
        return this.handleException(throwable, request);
    }

    private boolean isParameterCannotBeCoercedError(String message) {
        return message.contains("can not be coerced to the expected class");
    }

    ErrorResponse handleException(Throwable throwable, HttpServerRequest request) {
        log.error(throwable);
        return errorFactory.createErrorResponse(_500.getCode(), request.uri(), _500.getLabel());
    }

}
