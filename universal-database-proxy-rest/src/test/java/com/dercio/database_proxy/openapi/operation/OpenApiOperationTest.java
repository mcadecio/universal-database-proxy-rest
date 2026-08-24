package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.OpenApiColumnType;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.openapi.OpenApiType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers how {@link OpenApiOperation} turns a column into a schema, driven through {@link GetOperation}
 * because the behaviour lives in protected methods. The resolver is supplied inline so these tests do
 * not depend on any particular database's type names.
 */
class OpenApiOperationTest {

    private final GetOperation getOperation = new GetOperation(Clock.systemUTC());

    @Test
    void shouldDescribeAnArrayColumnWithItsElementType() {
        var schema = responseItemSchema("tags");

        assertAll(
                () -> assertEquals(OpenApiType.ARRAY, schema.getType()),
                // OpenAPI 3 requires "items" when "type" is "array"; without it the router rejects
                // the generated spec outright.
                () -> assertNotNull(schema.getItems()),
                () -> assertEquals(OpenApiType.STRING, schema.getItems().getType()),
                // The only array columns are CQL sets, whose elements are unique by definition.
                () -> assertEquals(Boolean.TRUE, schema.getUniqueItems())
        );
    }

    @Test
    void shouldFallBackToTheAnyRefForArraysOfAnUnsupportedElementType() {
        var schema = responseItemSchema("extras");

        assertAll(
                () -> assertEquals(OpenApiType.ARRAY, schema.getType()),
                () -> assertNotNull(schema.getItems()),
                () -> assertNull(schema.getItems().getType()),
                // Swagger expands the short name into a full component pointer.
                () -> assertEquals("#/components/schemas/ANY", schema.getItems().get$ref())
        );
    }

    @Test
    void shouldLeaveScalarColumnsUntouched() {
        var schema = responseItemSchema("title");

        assertAll(
                () -> assertEquals(OpenApiType.STRING, schema.getType()),
                () -> assertNull(schema.getItems()),
                () -> assertNull(schema.getUniqueItems())
        );
    }

    @Test
    void shouldDescribeAnArrayColumnsQueryParameterAsASingleElement() {
        // Set columns are filtered by membership, so the query parameter takes one element. An
        // array-typed query parameter would also break the proxy, which flattens the query string
        // into a single value per name.
        var parameter = queryParameter("tags");

        assertAll(
                () -> assertEquals(OpenApiType.STRING, parameter.getType()),
                () -> assertNull(parameter.getItems())
        );
    }

    @Test
    void shouldLeaveScalarQueryParametersUntouched() {
        assertEquals(OpenApiType.STRING, queryParameter("title").getType());
    }

    private Schema<?> responseItemSchema(String columnName) {
        var properties = operation().getResponses()
                .get("200")
                .getContent()
                .get("application/json")
                .getSchema()
                .getItems()
                .getProperties();

        assertTrue(properties.containsKey(columnName), "Expected a property for " + columnName);

        return (Schema<?>) properties.get(columnName);
    }

    private Schema<?> queryParameter(String columnName) {
        return operation().getParameters()
                .stream()
                .filter(parameter -> columnName.equals(parameter.getName()))
                .findFirst()
                .orElseThrow()
                .getSchema();
    }

    private Operation operation() {
        return getOperation.createOperation(new TableMetadata(
                "music", "music", "albums", List.of(
                column("album_id", OpenApiColumnType.scalar(OpenApiType.STRING), true),
                column("title", OpenApiColumnType.scalar(OpenApiType.STRING), false),
                column("tags", new OpenApiColumnType(OpenApiType.ARRAY, OpenApiType.STRING), false),
                column("extras", new OpenApiColumnType(OpenApiType.ARRAY, OpenApiType.ANY), false)
        )));
    }

    private ColumnMetadata column(String name, OpenApiColumnType openApiType, boolean primaryKey) {
        Function<String, OpenApiColumnType> resolver = dbType -> openApiType;

        return new ColumnMetadata(new JsonObject()
                .put("table_schema", "music")
                .put("table_name", "albums")
                .put("column_name", name)
                .put("data_type", "irrelevant")
                .put("is_nullable", primaryKey ? "NO" : "YES")
                .put("is_primary_key", primaryKey), resolver);
    }
}
