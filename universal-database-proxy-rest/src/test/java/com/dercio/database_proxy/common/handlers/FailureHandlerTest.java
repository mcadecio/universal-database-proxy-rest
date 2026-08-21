package com.dercio.database_proxy.common.handlers;

import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.core.servererrors.SyntaxError;
import com.dercio.database_proxy.common.error.ErrorFactory;
import com.dercio.database_proxy.common.error.ErrorResponse;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.time.format.DateTimeParseException;

import static com.simplaex.http.StatusCode._400;
import static com.simplaex.http.StatusCode._500;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureHandlerTest {

    private static final String URI = "/api/cars/1";

    @Mock
    private Mapper mapper;

    @Mock
    private RoutingContext event;

    @Mock
    private HttpServerRequest request;

    @Mock
    private HttpServerResponse response;

    private FailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new FailureHandler(mapper, new ErrorFactory());
        lenient().when(request.uri()).thenReturn(URI);
    }

    @Test
    void shouldMapUnknownExceptionsToA500() {
        var error = handleAndCaptureError(new RuntimeException("kaboom"));

        assertAll(
                () -> verify(response).setStatusCode(_500.getCode()),
                () -> verify(response).end("{encoded}"),
                () -> assertEquals(_500.getCode(), error.getCode()),
                () -> assertEquals(_500.getLabel(), error.getMessage()),
                () -> assertEquals(URI, error.getPath())
        );
    }

    @Test
    void shouldMapInconsistentStateExceptionToA400() {
        var error = handleAndCaptureError(new InconsistentStateException("Table requested does not exist"));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals(_400.getCode(), error.getCode()),
                () -> assertEquals("Table requested does not exist", error.getMessage())
        );
    }

    @Test
    void shouldMapIllegalStateExceptionToA400() {
        var error = handleAndCaptureError(new IllegalStateException("bad state"));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals("bad state", error.getMessage())
        );
    }

    @Test
    void shouldMapPgExceptionToA400() {
        var error = handleAndCaptureError(new PSQLException("duplicate key", PSQLState.UNIQUE_VIOLATION));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals("duplicate key", error.getMessage())
        );
    }

    @Test
    void shouldMapCassandraInvalidQueryExceptionToA400() {
        var error = handleAndCaptureError(
                new InvalidQueryException(null, "Undefined column name nope"));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals("Undefined column name nope", error.getMessage())
        );
    }

    @Test
    void shouldMapCassandraSyntaxErrorToA400() {
        var error = handleAndCaptureError(new SyntaxError(null, "line 1:0 no viable alternative"));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals("line 1:0 no viable alternative", error.getMessage())
        );
    }

    @Test
    void shouldMapDateTimeParseExceptionToA400() {
        var error = handleAndCaptureError(new DateTimeParseException("boom", "not-a-date", 0));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals("The value [not-a-date] is not a valid date", error.getMessage())
        );
    }

    @Test
    void shouldMapCoercionNoStackTraceThrowableToA400() {
        var error = handleAndCaptureError(
                new NoStackTraceThrowable("Value can not be coerced to the expected class"));

        assertAll(
                () -> verify(response).setStatusCode(_400.getCode()),
                () -> assertEquals("Value can not be coerced to the expected class", error.getMessage())
        );
    }

    @Test
    void shouldHandleDateTimeParseExceptionShouldDescribeTheOffendingValue() {
        var error = failureHandler.handleDateTimeParseException(
                new DateTimeParseException("boom", "13-2023", 0), request);

        assertEquals("The value [13-2023] is not a valid date", error.getMessage());
    }

    @Test
    void shouldHandleNoStackTraceThrowableShouldReturn400ForCoercionErrors() {
        var error = failureHandler.handleNoStackTraceThrowable(
                new NoStackTraceThrowable("Value 'x' can not be coerced to the expected class Integer"), request);

        assertAll(
                () -> assertEquals(_400.getCode(), error.getCode()),
                () -> assertEquals("Value 'x' can not be coerced to the expected class Integer", error.getMessage())
        );
    }

    @Test
    void shouldHandleNoStackTraceThrowableShouldFallBackTo500ForOtherMessages() {
        var error = failureHandler.handleNoStackTraceThrowable(
                new NoStackTraceThrowable("connection reset"), request);

        assertAll(
                () -> assertEquals(_500.getCode(), error.getCode()),
                () -> assertEquals(_500.getLabel(), error.getMessage())
        );
    }

    @Test
    void shouldHandleExceptionShouldReturnAGeneric500() {
        var error = failureHandler.handleException(new RuntimeException("secret internals"), request);

        assertAll(
                () -> assertEquals(_500.getCode(), error.getCode()),
                () -> assertEquals(_500.getLabel(), error.getMessage()),
                () -> assertEquals(URI, error.getPath())
        );
    }

    @Test
    void shouldHandleBodyProcessorExceptionShouldRewriteNullableValidationMessages() {
        var validationException = validationException("nullable", "input cannot be null",
                "", JsonPointer.from("/name"));
        var bodyProcessorException = new Throwable("wrapper", validationException);

        var error = failureHandler.handleBodyProcessorException(bodyProcessorException, request);

        assertAll(
                () -> assertEquals(_400.getCode(), error.getCode()),
                () -> assertEquals("name cannot be null", error.getMessage())
        );
    }

    @Test
    void shouldHandleBodyProcessorExceptionShouldFallBackToBodyWhenNoPropertyScope() {
        var validationException = validationException("nullable", "input cannot be null",
                "", JsonPointer.create());
        var bodyProcessorException = new Throwable("wrapper", validationException);

        var error = failureHandler.handleBodyProcessorException(bodyProcessorException, request);

        assertEquals("body cannot be null", error.getMessage());
    }

    @Test
    void shouldHandleBodyProcessorExceptionShouldRewriteTypeValidationMessages() {
        var validationException = validationException("type", "input don't match type STRING",
                42, JsonPointer.from("/age"));
        var bodyProcessorException = new Throwable("wrapper", validationException);

        var error = failureHandler.handleBodyProcessorException(bodyProcessorException, request);

        assertAll(
                () -> assertEquals(_400.getCode(), error.getCode()),
                () -> assertEquals("property 'age' with value \"42\" is not a valid STRING", error.getMessage())
        );
    }

    @Test
    void shouldHandleBodyProcessorExceptionShouldUseTheRawMessageForNonValidationCauses() {
        var error = failureHandler.handleBodyProcessorException(new Throwable("malformed body"), request);

        assertAll(
                () -> assertEquals(_400.getCode(), error.getCode()),
                () -> assertEquals("malformed body", error.getMessage())
        );
    }

    @Test
    void shouldHandleParameterProcessorExceptionShouldReturn400WithTheRawMessage() {
        var error = failureHandler.handleParameterProcessorException(new Throwable("bad param"), request);

        assertAll(
                () -> assertEquals(_400.getCode(), error.getCode()),
                () -> assertEquals("bad param", error.getMessage())
        );
    }

    private ErrorResponse handleAndCaptureError(Throwable failure) {
        when(event.failure()).thenReturn(failure);
        when(event.request()).thenReturn(request);
        when(event.response()).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(response);
        when(mapper.encode(any())).thenReturn("{encoded}");

        failureHandler.handle(event);

        var captor = ArgumentCaptor.forClass(ErrorResponse.class);
        verify(mapper).encode(captor.capture());
        return captor.getValue();
    }

    private ValidationException validationException(String keyword, String message, Object input, JsonPointer scope) {
        ValidationException validationException = org.mockito.Mockito.mock(ValidationException.class);
        lenient().when(validationException.keyword()).thenReturn(keyword);
        lenient().when(validationException.getMessage()).thenReturn(message);
        lenient().when(validationException.input()).thenReturn(input);
        lenient().when(validationException.inputScope()).thenReturn(scope);
        return validationException;
    }
}
