package com.dercio.database_proxy.openapi;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.google.inject.Inject;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static com.simplaex.http.StatusCode._200;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class OpenApiCreator {
    private static final String OPEN_API_TAG = "Open API";
    private static final String OPEN_API_PATH = "/openapi";
    private static final String API_TITLE = "Auto Generated Open API";
    private static final String API_VERSION = "1.0.1";
    private static final String SUCCESS_CODE = String.valueOf(_200.getCode());

    private final OpenApiPathsCreator pathCreator;

    public OpenAPI create(List<TableMetadata> tableMetadataList) {
        var paths = pathCreator.createPaths(tableMetadataList);
        var openApiItem = new PathItem();
        openApiItem.setGet(createOpenApiGetOperation());
        paths.addPathItem(OPEN_API_PATH, openApiItem);

        return new OpenAPI()
                .info(createApiInfo())
                .servers(List.of(createDefaultServer()))
                .tags(createTags(tableMetadataList))
                .paths(paths)
                .components(createComponents());
    }

    private List<Tag> createTags(List<TableMetadata> tableMetadataList) {
        return tableMetadataList.stream()
                .map(TableMetadata::getTableName)
                .map(tableName -> new Tag().name(tableName))
                .toList();
    }

    private Components createComponents() {
        var anySchema = new Schema<>();
        anySchema.description("Can be anything: string, number, array, object, etc., including `null`");

        var errorResponse = createErrorResponseSchema();

        return new Components()
                .addSchemas("ANY", anySchema)
                .addSchemas("ErrorResponse", errorResponse);
    }

    private ObjectSchema createErrorResponseSchema() {
        var schema = new ObjectSchema();
        schema.addProperty("timestamp", new StringSchema().example("2023-06-11T12:11:25"));
        schema.addProperty("path", new StringSchema().description("The url path of the error").example("/cars/1"));
        schema.addProperty("message", new StringSchema().description("The error message").example("Not found"));
        schema.addProperty("code", new IntegerSchema().description("The HTTP status code").example(404));
        return schema;
    }

    private Server createDefaultServer() {
        return new Server()
                .url("/")
                .description("Optional server description, e.g. Main (production) server");
    }

    private Info createApiInfo() {
        return new Info()
                .title(API_TITLE)
                .description("Optional multiline or single-line description in [CommonMark](http://commonmark.org/help/) or HTML.")
                .version(API_VERSION);
    }

    private Operation createOpenApiGetOperation() {
        var operation = new Operation()
                .summary("Get OpenAPI")
                .description(API_TITLE)
                .operationId("getOpenApi")
                .tags(Collections.singletonList(OPEN_API_TAG));
        operation.addExtension("x-metadata", Collections.emptyMap());

        var objectSchema = new ObjectSchema().example(createExampleOpenApiJson());
        var apiResponse = createJsonResponse("The Open API Spec", objectSchema);
        operation.setResponses(createSuccessResponses(apiResponse));

        return operation;
    }

    private String createExampleOpenApiJson() {
        var openAPI = new OpenAPI()
                .info(createApiInfo())
                .servers(List.of(createDefaultServer()))
                .paths(createExamplePaths());
        return Json.pretty(openAPI);
    }

    private Paths createExamplePaths() {
        var paths = new Paths();
        var pathItem = new PathItem().get(createExampleGetOperation());
        paths.addPathItem("/cars", pathItem);
        return paths;
    }

    private Operation createExampleGetOperation() {
        var getOperation = new Operation()
                .summary("Returns a list of cars")
                .operationId("get_cars");

        var carSchema = createExampleCarSchema();
        var arraySchema = new ArraySchema().items(carSchema);
        var apiResponse = createJsonResponse("A JSON array of cars", arraySchema);
        getOperation.setResponses(createSuccessResponses(apiResponse));

        return getOperation;
    }

    private ObjectSchema createExampleCarSchema() {
        var properties = new LinkedHashMap<String, Schema>();
        properties.put("id", new ObjectSchema().type("integer").example(1));
        properties.put("name", new ObjectSchema().type("string").example("Ferrari"));
        properties.put("doors", new ObjectSchema().type("integer").example(5));

        ObjectSchema schema = new ObjectSchema();
        schema.properties(properties);
        schema.required(new ArrayList<>(properties.keySet()));
        return schema;
    }

    private ApiResponse createJsonResponse(String description, Schema<?> schema) {
        var mediaType = new MediaType().schema(schema);
        var content = new Content().addMediaType(APPLICATION_JSON.toString(), mediaType);
        return new ApiResponse().description(description).content(content);
    }

    private ApiResponses createSuccessResponses(ApiResponse apiResponse) {
        return new ApiResponses().addApiResponse(SUCCESS_CODE, apiResponse);
    }
}
