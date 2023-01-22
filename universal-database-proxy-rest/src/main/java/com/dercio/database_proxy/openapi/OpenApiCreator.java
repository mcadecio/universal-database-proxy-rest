package com.dercio.database_proxy.openapi;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

import static com.simplaex.http.StatusCode._200;
import static com.simplaex.http.StatusCode._201;
import static com.simplaex.http.StatusCode._204;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

@Log4j2
public class OpenApiCreator {

    private static final String AUTO_GENERATED = "Auto Generated";
    private static final String BY_ID_OPERATION = "_by_id";
    private static final String BY_ID = " by id";

    private OpenApiCreator() {
    }

    public static OpenAPI create(List<TableMetadata> tableMetadataList) {
        return new OpenAPI()
                .info(generateInfo())
                .servers(List.of(generateServer()))
                .tags(tableMetadataList.stream()
                        .map(TableMetadata::getTableName)
                        .map(tableName -> new Tag().name(tableName))
                        .collect(Collectors.toList()))
                .paths(generatePaths(tableMetadataList));
    }

    private static Schema<?> createSchemaFromColumns(List<ColumnMetadata> columns) {
        var properties = columns.stream()
                .map(column -> Map.entry(column.getColumnName(), new ObjectSchema()
                        .type(column.getOpenApiType())
                        .nullable(column.isNullable())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (schema, schema2) -> schema, LinkedHashMap::new
                ));

        var requiredProperties = columns
                .stream()
                .filter(column -> !column.isNullable())
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.toList());

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.properties(properties);

        if (!requiredProperties.isEmpty()) {
            objectSchema.required(requiredProperties);
        }

        return objectSchema;
    }

    private static Paths generatePaths(List<TableMetadata> tableMetadataList) {
        Paths paths = new Paths();

        tableMetadataList.forEach(tableMetadata -> {
            PathItem pathItem = new PathItem();

            pathItem.setGet(generateGetOperation(tableMetadata));
            pathItem.setPost(generatePostOperation(tableMetadata));

            var pathItemById = new PathItem();
            pathItemById.setGet(generateGetByIdOperation(tableMetadata));
            pathItemById.setPut(generatePutByIdOperation(tableMetadata));
            pathItemById.setDelete(generateDeleteOperation(tableMetadata));

            paths.addPathItem("/" + tableMetadata.getTableName(), pathItem);
            paths.addPathItem(String.format("/%s/{%s}", tableMetadata.getTableName(), tableMetadata.getPkColumnName()), pathItemById);
        });

        var openApiItem = new PathItem();
        openApiItem.setGet(generateOpenApiGetOperation());
        paths.addPathItem("/openapi", openApiItem);

        return paths;
    }

    private static Operation generateOpenApiGetOperation() {
        Operation getOperation = new Operation();
        getOperation.setSummary("Get OpenAPI");
        getOperation.setDescription("Auto Generated Open API");
        getOperation.setOperationId("getOpenApi");
        getOperation.setTags(Collections.singletonList("Open API"));
        getOperation.addExtension("x-metadata", Collections.emptyMap());

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

        getOperation.setResponses(apiResponses);

        return getOperation;
    }

    private static Parameter createPkPathParameter(TableMetadata tableMetadata) {
        var primaryKeyColumn = tableMetadata.getPrimaryKeyColumn();
        return new Parameter()
                .name(primaryKeyColumn.getColumnName())
                .in("path")
                .required(true)
                .schema(new ObjectSchema().type(primaryKeyColumn.getOpenApiType()));
    }

    private static Operation generateGetByIdOperation(TableMetadata tableMetadata) {
        Operation getOperation = new Operation();
        getOperation.setSummary("Get a  " + tableMetadata.getTableName() + BY_ID);
        getOperation.setDescription(AUTO_GENERATED);
        getOperation.setOperationId("get_" + tableMetadata.getTableName() + BY_ID_OPERATION);
        getOperation.setTags(Collections.singletonList(tableMetadata.getTableName()));
        addOperationMetadata(getOperation, tableMetadata);

        var objectSchema = createSchemaFromColumns(tableMetadata.getColumns());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(objectSchema);

        Content content = new Content();
        content.addMediaType(APPLICATION_JSON.toString(), mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON representing " + tableMetadata.getTableName());
        apiResponse.setContent(content);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);


        Parameter pathParameter = createPkPathParameter(tableMetadata);
        getOperation.addParametersItem(pathParameter);
        getOperation.setResponses(apiResponses);

        return getOperation;
    }

    private static Operation generatePutByIdOperation(TableMetadata tableMetadata) {
        Operation putOperation = new Operation();
        putOperation.setSummary("Update a " + tableMetadata.getTableName() + BY_ID);
        putOperation.setDescription(AUTO_GENERATED);
        putOperation.setOperationId("update_" + tableMetadata.getTableName() + BY_ID_OPERATION);
        putOperation.setTags(Collections.singletonList(tableMetadata.getTableName()));
        addOperationMetadata(putOperation, tableMetadata);

        var objectSchema = createSchemaFromColumns(tableMetadata.getColumns());

        Content requestBodycontent = new Content();
        requestBodycontent.addMediaType(APPLICATION_JSON.toString(), new MediaType().schema(objectSchema));

        RequestBody requestBody = new RequestBody();
        requestBody.required(true);
        requestBody.content(requestBodycontent);


        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("The " + tableMetadata.getTableName() + " has been updated.");


        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_204.getCode()), apiResponse);

        Parameter pathParameter = createPkPathParameter(tableMetadata);
        putOperation.addParametersItem(pathParameter);
        putOperation.setRequestBody(requestBody);
        putOperation.setResponses(apiResponses);

        return putOperation;
    }

    private static Operation generateDeleteOperation(TableMetadata tableMetadata) {
        Operation deleteOperation = new Operation();
        deleteOperation.setSummary("Delete " + tableMetadata.getTableName() + BY_ID);
        deleteOperation.setDescription(AUTO_GENERATED);
        deleteOperation.setOperationId("delete_" + tableMetadata.getTableName() + BY_ID_OPERATION);
        deleteOperation.setTags(Collections.singletonList(tableMetadata.getTableName()));
        addOperationMetadata(deleteOperation, tableMetadata);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), new ApiResponse()
                .description("Deleted " + tableMetadata.getTableName() + " successfully."));

        Parameter pathParameter = createPkPathParameter(tableMetadata);
        deleteOperation.addParametersItem(pathParameter);
        deleteOperation.responses(apiResponses);

        return deleteOperation;
    }

    private static Operation generateGetOperation(TableMetadata tableMetadata) {
        Operation getOperation = new Operation();
        getOperation.setSummary("Returns a list of " + tableMetadata.getTableName());
        getOperation.setDescription(AUTO_GENERATED);
        getOperation.setOperationId("get_" + tableMetadata.getTableName());
        getOperation.setTags(Collections.singletonList(tableMetadata.getTableName()));
        addOperationMetadata(getOperation, tableMetadata);
        getOperation.parameters(generateGetOperationQueryParams(tableMetadata.getColumns()));

        var objectSchema = createSchemaFromColumns(tableMetadata.getColumns());

        ArraySchema schema = new ArraySchema()
                .items(objectSchema);

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType(APPLICATION_JSON.toString(), mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON array of " + tableMetadata.getTableName());
        apiResponse.setContent(content);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        getOperation.setResponses(apiResponses);
        return getOperation;
    }

    private static List<Parameter> generateGetOperationQueryParams(List<ColumnMetadata> columns) {
        return columns.stream()
                .map(column -> new Parameter()
                        .name(column.getColumnName())
                        .in("query")
                        .schema(new Schema<>().type(column.getOpenApiType()))
                )
                .collect(Collectors.toList());
    }

    private static Operation generatePostOperation(TableMetadata tableMetadata) {
        Operation postOperation = new Operation();
        postOperation.setSummary("Creates a new " + tableMetadata.getTableName());
        postOperation.setDescription(AUTO_GENERATED);
        postOperation.setOperationId("create_" + tableMetadata.getTableName());
        postOperation.setTags(Collections.singletonList(tableMetadata.getTableName()));
        addOperationMetadata(postOperation, tableMetadata);

        var objectSchema = createSchemaFromColumns(tableMetadata.getColumns());

        Content requestBodycontent = new Content();
        requestBodycontent.addMediaType(APPLICATION_JSON.toString(), new MediaType().schema(objectSchema));

        RequestBody requestBody = new RequestBody();
        requestBody.required(true);
        requestBody.content(requestBodycontent);


        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A new " + tableMetadata.getTableName() + " has been created.");
        Header locationHeader = new Header()
                .description("The URI of the resource created.")
                .schema(new StringSchema());
        apiResponse.addHeaderObject("Location", locationHeader);

        // TODO: Add bad request response


        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_201.getCode()), apiResponse);

        postOperation.setRequestBody(requestBody);
        postOperation.setResponses(apiResponses);
        return postOperation;
    }

    private static Server generateServer() {
        Server server = new Server();
        server.setUrl("/");
        server.setDescription("Optional server description, e.g. Main (production) server");
        return server;
    }

    private static Info generateInfo() {
        Info info = new Info();
        info.setTitle("Auto Generated Open API");
        info.setDescription("Optional multiline or single-line description in [CommonMark](http://commonmark.org/help/) or HTML.");
        info.setVersion("1.0.1");
        return info;
    }

    private static void addOperationMetadata(Operation operation, TableMetadata tableMetadata) {
        operation.addExtension("x-metadata",
                new JsonObject()
                        .put("database", tableMetadata.getDatabaseName())
                        .put("schema", tableMetadata.getSchemaName())
                        .put("table", tableMetadata.getTableName()).getMap());
    }

    private static String exampleOpenApi() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(generateInfo());
        openAPI.setServers(List.of(generateServer()));
        openAPI.setPaths(generateExamplePaths());
        return Json.pretty(openAPI);
    }

    private static Paths generateExamplePaths() {
        Paths paths = new Paths();
        PathItem pathItem = new PathItem();

        pathItem.setGet(generateExampleGetOperation());

        paths.addPathItem("/cars", pathItem);
        return paths;
    }

    private static Operation generateExampleGetOperation() {
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
