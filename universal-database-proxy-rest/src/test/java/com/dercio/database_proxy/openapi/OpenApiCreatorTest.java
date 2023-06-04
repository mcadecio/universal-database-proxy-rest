package com.dercio.database_proxy.openapi;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.openapi.operation.*;
import io.swagger.v3.core.util.Json;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiCreatorTest {

    private final OpenApiPathsCreator pathCreator = new OpenApiPathsCreator(
            new GetOperation(),
            new PostOperation(),
            new GetByIdOperation(),
            new PutByIdOperation(),
            new DeleteByIdOperation()
    );
    private final OpenApiCreator openApiCreator = new OpenApiCreator(pathCreator);

    @Test
    void shouldCreateCarsOpenApi() throws Exception {
        TableMetadata carsTableMetadata = loadCarsTableMetadata("cars_table_metadata.json");
        var expectedOpenApi = loadOpenApi("cars_table_openapi.json").getMap();

        var openAPI = openApiCreator.create(List.of(carsTableMetadata));

        assertEquals(Json.pretty(expectedOpenApi), Json.pretty(openAPI));
    }

    @Test
    void shouldCreateBugsOpenApi() throws Exception {
        TableMetadata carsTableMetadata = loadCarsTableMetadata("bugs_table_metadata.json");
        var expectedOpenApi = loadOpenApi("bugs_table_openapi.json").getMap();

        var openAPI = openApiCreator.create(List.of(carsTableMetadata));

        assertEquals(Json.pretty(expectedOpenApi), Json.pretty(openAPI));
    }

    private JsonObject loadOpenApi(String file) throws URISyntaxException, IOException {
        var uri = getResource(file).toURI();
        return new JsonObject(Files.readString(Paths.get(uri)));
    }

    private TableMetadata loadCarsTableMetadata(String file) throws IOException, URISyntaxException {
        var resource = getResource(file);
        var rawMetadata = new JsonObject(Files.readString(Paths.get(resource.toURI())));
        var columns = rawMetadata.getJsonArray("columns")
                .stream()
                .map(JsonObject.class::cast)
                .map(ColumnMetadata::new)
                .collect(Collectors.toList());
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