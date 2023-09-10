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

import java.util.*;
import java.util.stream.Collectors;

import static com.simplaex.http.StatusCode._200;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class OpenApiCreator {

    private final OpenApiPathsCreator pathCreator;

    public OpenAPI create(List<TableMetadata> tableMetadataList) {
        var paths = pathCreator.createPaths(tableMetadataList);
        var openApiItem = new PathItem();
        openApiItem.setGet(generateOpenApiGetOperation());
        paths.addPathItem("/openapi", openApiItem);

        return new OpenAPI()
                .info(generateInfo())
                .servers(List.of(generateServer()))
                .tags(tableMetadataList.stream()
                        .map(TableMetadata::getTableName)
                        .map(tableName -> new Tag().name(tableName))
                        .toList())
                .paths(paths)
                .components(generateComponents());
    }

    private Components generateComponents() {
        var schema = new Schema<>();
        schema.description("Can be anything: string, number, array, object, etc., including `null`");

        var errorResponse = new ObjectSchema();
        errorResponse.addProperty("timestamp", new StringSchema().example("2023-06-11T12:11:25"))
                .addProperty("path", new StringSchema()
                        .description("The url path of the error")
                        .example("/cars/1"))
                .addProperty("message", new StringSchema()
                        .description("The error message")
                        .example("Not found"))
                .addProperty("code", new IntegerSchema()
                        .description("The HTTP status code")
                        .example(404));

        return new Components()
                .addSchemas("ANY", schema)
                .addSchemas("ErrorResponse", errorResponse);
    }

    private Server generateServer() {
        Server server = new Server();
        server.setUrl("/");
        server.setDescription("Optional server description, e.g. Main (production) server");
        return server;
    }

    private Info generateInfo() {
        Info info = new Info();
        info.setTitle("Auto Generated Open API");
        info.setDescription("Optional multiline or single-line description in [CommonMark](http://commonmark.org/help/) or HTML.");
        info.setVersion("1.0.1");
        return info;
    }

    private Operation generateOpenApiGetOperation() {
        Operation operation = new Operation();
        operation.setSummary("Get OpenAPI");
        operation.setDescription("Auto Generated Open API");
        operation.setOperationId("getOpenApi");
        operation.setTags(Collections.singletonList("Open API"));
        operation.addExtension("x-metadata", Collections.emptyMap());

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.example(exampleOpenApi());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(objectSchema);

        Content content = new Content();
        content.addMediaType(APPLICATION_JSON.toString(), mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("The Open API Spec");
        apiResponse.setContent(content);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        operation.setResponses(apiResponses);

        return operation;
    }

    private String exampleOpenApi() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(generateInfo());
        openAPI.setServers(List.of(generateServer()));
        openAPI.setPaths(generateExamplePaths());
        return Json.pretty(openAPI);
    }


    private Paths generateExamplePaths() {
        Paths paths = new Paths();
        PathItem pathItem = new PathItem();

        pathItem.setGet(generateExampleGetOperation());

        paths.addPathItem("/cars", pathItem);
        return paths;
    }

    private Operation generateExampleGetOperation() {
        Operation getOperation = new Operation();
        getOperation.setSummary("Returns a list of cars");
        getOperation.setOperationId("get_cars");

        var properties = new LinkedHashMap<String, Schema>();

        properties.put("id", new ObjectSchema().type("integer").example(1));
        properties.put("name", new ObjectSchema().type("string").example("Ferrari"));
        properties.put("doors", new ObjectSchema().type("integer").example(5));

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.properties(properties);
        objectSchema.required(new ArrayList<>(properties.keySet()));

        ArraySchema schema = new ArraySchema()
                .items(objectSchema);

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType(APPLICATION_JSON.toString(), mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON array of cars");
        apiResponse.setContent(content);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        getOperation.setResponses(apiResponses);
        return getOperation;
    }

}
