package com.dercio.database_proxy.openapi;

import com.dercio.database_proxy.common.database.Table;
import com.dercio.database_proxy.common.database.TableColumn;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

import static com.simplaex.http.StatusCode._200;
import static com.simplaex.http.StatusCode._204;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class OpenApiCreator {

    private OpenApiCreator() {
    }

    public static OpenAPI create(List<Table> tableList) {
        return new OpenAPI()
                .info(generateInfo())
                .servers(List.of(generateServer()))
                .tags(tableList.stream()
                        .map(Table::getTableName)
                        .map(tableName -> new Tag().name(tableName))
                        .collect(Collectors.toList()))
                .paths(generatePaths(tableList));
    }

    private static Schema createSchemaFromColumns(List<TableColumn> columns) {
        Map<String, Schema> properties = columns.stream()
                .map(column -> Map.entry(column.getColumnName(), new ObjectSchema()
                        .type(column.getDataType())
                        .nullable(column.isNullable())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var requiredProperties = columns
                .stream()
                .filter(column -> !column.isNullable())
                .map(TableColumn::getColumnName)
                .collect(Collectors.toList());

        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.properties(properties);
        objectSchema.required(requiredProperties);
        return objectSchema;
    }

    private static Paths generatePaths(List<Table> tableList) {
        Paths paths = new Paths();

        tableList.forEach(table -> {
            PathItem pathItem = new PathItem();

            pathItem.setGet(generateGetOperation(table));
            pathItem.setPost(generatePostOperation(table));

            var pathItemById = new PathItem();
            pathItemById.setGet(generateGetByIdOperation(table));
            pathItemById.setPut(generatePutByIdOperation(table));
            pathItemById.setDelete(generateDeleteOperation(table));

            paths.addPathItem("/" + table.getTableName(), pathItem);
            paths.addPathItem(String.format("/%s/{%s}", table.getTableName(), table.getPkColumnName()), pathItemById);
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

    private static Parameter createPkPathParameter(Table table) {
        return table.getColumns()
                .stream()
                .filter(column -> column.getColumnName().equals(table.getPkColumnName()))
                .findAny()
                .map(column -> new Parameter()
                        .name(column.getColumnName())
                        .in("path")
                        .required(true)
                        .schema(new ObjectSchema().type(column.getDataType())))
                .orElseThrow(); // TODO: Throw dedicated exception
    }

    private static Operation generateGetByIdOperation(Table table) {
        Operation getOperation = new Operation();
        getOperation.setSummary("Get a  " + table.getTableName() + " by id");
        getOperation.setDescription("Auto Generated");
        getOperation.setOperationId("get_" + table.getTableName() + "_by_id");
        getOperation.setTags(Collections.singletonList(table.getTableName()));
        addOperationMetadata(getOperation, table);

        var objectSchema = createSchemaFromColumns(table.getColumns());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(objectSchema);

        Content content = new Content();
        content.addMediaType(APPLICATION_JSON.toString(), mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON representing " + table.getTableName());
        apiResponse.setContent(content);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);


        Parameter pathParameter = createPkPathParameter(table);
        getOperation.addParametersItem(pathParameter);
        getOperation.setResponses(apiResponses);

        return getOperation;
    }

    private static Operation generatePutByIdOperation(Table table) {
        Operation putOperation = new Operation();
        putOperation.setSummary("Update a " + table.getTableName() + " by id");
        putOperation.setDescription("Auto Generated");
        putOperation.setOperationId("update_" + table.getTableName() + "_by_id");
        putOperation.setTags(Collections.singletonList(table.getTableName()));
        addOperationMetadata(putOperation, table);

        var objectSchema = createSchemaFromColumns(table.getColumns());

        Content requestBodycontent = new Content();
        requestBodycontent.addMediaType(APPLICATION_JSON.toString(), new MediaType().schema(objectSchema));

        RequestBody requestBody = new RequestBody();
        requestBody.required(true);
        requestBody.content(requestBodycontent);


        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("The " + table.getTableName() + " has been updated.");


        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_204.getCode()), apiResponse);

        Parameter pathParameter = createPkPathParameter(table);
        putOperation.addParametersItem(pathParameter);
        putOperation.setRequestBody(requestBody);
        putOperation.setResponses(apiResponses);

        return putOperation;
    }

    private static Operation generateDeleteOperation(Table table) {
        Operation deleteOperation = new Operation();
        deleteOperation.setSummary("Delete " + table.getTableName() + " by id");
        deleteOperation.setDescription("Auto Generated");
        deleteOperation.setOperationId("delete_" + table.getTableName() + "_by_id");
        deleteOperation.setTags(Collections.singletonList(table.getTableName()));
        addOperationMetadata(deleteOperation, table);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), new ApiResponse()
                .description("Deleted " + table.getTableName() + " successfully."));

        Parameter pathParameter = createPkPathParameter(table);
        deleteOperation.addParametersItem(pathParameter);
        deleteOperation.responses(apiResponses);

        return deleteOperation;
    }

    private static Operation generateGetOperation(Table table) {
        Operation getOperation = new Operation();
        getOperation.setSummary("Returns a list of " + table.getTableName());
        getOperation.setDescription("Auto Generated");
        getOperation.setOperationId("get_" + table.getTableName());
        getOperation.setTags(Collections.singletonList(table.getTableName()));
        addOperationMetadata(getOperation, table);

        var objectSchema = createSchemaFromColumns(table.getColumns());

        ArraySchema schema = new ArraySchema()
                .items(objectSchema);

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType(APPLICATION_JSON.toString(), mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON array of " + table.getTableName());
        apiResponse.setContent(content);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        getOperation.setResponses(apiResponses);
        return getOperation;
    }

    private static Operation generatePostOperation(Table table) {
        Operation postOperation = new Operation();
        postOperation.setSummary("Creates a new " + table.getTableName());
        postOperation.setDescription("Auto Generated");
        postOperation.setOperationId("create_" + table.getTableName());
        postOperation.setTags(Collections.singletonList(table.getTableName()));
        addOperationMetadata(postOperation, table);

        var objectSchema = createSchemaFromColumns(table.getColumns());

        Content requestBodycontent = new Content();
        requestBodycontent.addMediaType(APPLICATION_JSON.toString(), new MediaType().schema(objectSchema));

        RequestBody requestBody = new RequestBody();
        requestBody.required(true);
        requestBody.content(requestBodycontent);


        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A new " + table.getTableName() + " has been created.");

        // TODO: Add bad request response


        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_204.getCode()), apiResponse);

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

    private static void addOperationMetadata(Operation operation, Table table) {
        operation.addExtension("x-metadata",
                new JsonObject()
                        .put("database", table.getDatabaseName())
                        .put("schema", table.getSchemaName())
                        .put("table", table.getTableName()).getMap());
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
