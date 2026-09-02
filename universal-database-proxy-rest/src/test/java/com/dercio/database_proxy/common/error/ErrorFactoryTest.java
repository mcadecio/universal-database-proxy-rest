package com.dercio.database_proxy.common.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorFactoryTest {

    private final ErrorFactory errorFactory = new ErrorFactory();

    @Test
    void shouldBuildErrorResponseFromAnException() {
        var response = errorFactory.createErrorResponse(500, "/api/cars", new RuntimeException("boom"));

        assertAll(
                () -> assertEquals(500, response.getCode()),
                () -> assertEquals("/api/cars", response.getPath()),
                () -> assertEquals("boom", response.getMessage()),
                () -> assertTrue(response.getErrors().isEmpty())
        );
    }

    @Test
    void shouldBuildErrorResponseFromAMessageWithAnEmptyErrorList() {
        var response = errorFactory.createErrorResponse(404, "/api/cars/1", "Not Found");

        assertAll(
                () -> assertEquals(404, response.getCode()),
                () -> assertEquals("/api/cars/1", response.getPath()),
                () -> assertEquals("Not Found", response.getMessage()),
                () -> assertTrue(response.getErrors().isEmpty())
        );
    }

    @Test
    void shouldBuildErrorResponseCarryingFieldLevelErrors() {
        var fieldErrors = List.of(
                ErrorField.of("name", "must not be blank"),
                ErrorField.of("age", "must be positive")
        );

        var response = errorFactory.createErrorResponse(400, "/api/people", "Validation failed", fieldErrors);

        assertAll(
                () -> assertEquals(400, response.getCode()),
                () -> assertEquals("Validation failed", response.getMessage()),
                () -> assertEquals(2, response.getErrors().size()),
                () -> assertEquals("name", response.getErrors().getFirst().getFieldName())
        );
    }
}
