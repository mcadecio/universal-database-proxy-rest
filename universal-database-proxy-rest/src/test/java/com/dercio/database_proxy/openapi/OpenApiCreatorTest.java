package com.dercio.database_proxy.openapi;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.openapi.operation.*;
import io.swagger.v3.core.util.Json;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiCreatorTest {

    @Mock
    private Clock clock;

    private OpenApiCreator openApiCreator;

    @BeforeEach
    void setUp() {
        OpenApiPathsCreator pathCreator = new OpenApiPathsCreator(
                new GetOperation(clock),
                new PostOperation(clock),
                new GetByIdOperation(clock),
                new PutByIdOperation(clock),
                new DeleteByIdOperation(clock),
                new DeleteOperation(clock)
        );
        openApiCreator = new OpenApiCreator(pathCreator);

        when(clock.instant()).thenReturn(Instant.ofEpochSecond(1687601548L));
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "cars_table_metadata.json,cars_table_openapi.json",
            "bugs_table_metadata.json,bugs_table_openapi.json",
            "students_table_metadata.json,students_table_openapi.json"
    })
    void shouldMatchExpectedOpenApi(String input, String expectedOutput) throws Exception {
        TableMetadata tableMetadata = loadTableMetadata(input);
        var expectedOpenApi = loadOpenApi(expectedOutput).getMap();

        var openAPI = openApiCreator.create(List.of(tableMetadata));

        assertEquals(Json.pretty(expectedOpenApi), Json.pretty(openAPI));
    }

    private JsonObject loadOpenApi(String file) throws URISyntaxException, IOException {
        var uri = getResource(file).toURI();
        return new JsonObject(Files.readString(Paths.get(uri)));
    }

    private TableMetadata loadTableMetadata(String file) throws IOException, URISyntaxException {
        var resource = getResource(file);
        var rawMetadata = new JsonObject(Files.readString(Paths.get(resource.toURI())));
        var columns = rawMetadata.getJsonArray("columns")
                .stream()
                .map(JsonObject.class::cast)
                .map(ColumnMetadata::new)
                .toList();
        return new TableMetadata(
                rawMetadata.getString("databaseName"),
                rawMetadata.getString("schemaName"),
                rawMetadata.getString("tableName"),
                columns
        );
    }

    private URL getResource(String name) {
        return getClass().getClassLoader().getResource(name);
    }
}