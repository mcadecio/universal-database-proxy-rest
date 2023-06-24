package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.error.ErrorResponse;
import com.dercio.database_proxy.openapi.OpenApiType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.simplaex.http.StatusCode.*;
import static com.simplaex.http.StatusCode._500;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class OpenApiOperation {

    private static final String AUTO_GENERATED = "Auto Generated";
    private static final String ERROR_RESPONSE_SCHEMA_REF = "ErrorResponse";

    protected final Clock clock;

    public Operation createOperation(TableMetadata tableMetadata) {
        Operation operation = new Operation();
        operation.setSummary(operationSummary(tableMetadata));
        operation.setDescription(AUTO_GENERATED);
        operation.setOperationId(operationId(tableMetadata));
        operation.addTagsItem(tableMetadata.getTableName());
        operation.addExtension("x-metadata", new JsonObject()
                .put("database", tableMetadata.getDatabaseName())
                .put("schema", tableMetadata.getSchemaName())
                .put("table", tableMetadata.getTableName()).getMap());
        operation.parameters(operationParameters(tableMetadata));
        operation.requestBody(operationRequestBody(tableMetadata));
        operation.responses(operationApiResponses(tableMetadata));
        return operation;
    }

    protected abstract String operationSummary(TableMetadata tableMetadata);

    protected abstract String operationId(TableMetadata tableMetadata);

    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        ApiResponses apiResponses = new ApiResponses();
        String errorUrl = "/" + tableMetadata.getTableName();
        apiResponses.addApiResponse(String.valueOf(_400.getCode()), badRequestApiResponse(errorUrl));
        apiResponses.addApiResponse(String.valueOf(_404.getCode()), notFoundApiResponse());
        apiResponses.addApiResponse(String.valueOf(_500.getCode()), internalServerErrorApiResponse(errorUrl));
        return apiResponses;
    }

    protected List<Parameter> operationParameters(TableMetadata tableMetadata) {
        return Collections.emptyList();
    }

    protected RequestBody operationRequestBody(TableMetadata tableMetadata) {
        return null;
    }

    protected Schema<Object> createSchemaFromColumns(List<ColumnMetadata> columns) {
        Map<String, Schema> properties = columns.stream()
                .map(column -> {
                    var schema = schemaFromColumn(column);
                    return Map.entry(column.getColumnName(), schema);
                })
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

    protected Schema<Object> schemaFromColumn(ColumnMetadata column) {
        ObjectSchema schema = new ObjectSchema();
        schema.type(column.getOpenApiType());
        schema.nullable(column.isNullable());

        if (OpenApiType.ANY.equals(column.getOpenApiType())) {
            schema.type(null).$ref(column.getOpenApiType());
        }
        return schema;
    }

    protected Content jsonContent(Schema<Object> schema) {
        return new Content().addMediaType("application/json", new MediaType().schema(schema));
    }

    protected Content jsonContent(Schema<Object> schema, Object example) {
        return new Content().addMediaType("application/json", new MediaType().example(example).schema(schema));
    }

    private ApiResponse notFoundApiResponse() {
        var schema = new Schema<>();
        schema.$ref(ERROR_RESPONSE_SCHEMA_REF);
        var example = ErrorResponse.builder()
                .timestamp(LocalDateTime.now(clock))
                .code(404)
                .message("Not Found")
                .path("/chairs")
                .build();
        return new ApiResponse()
                .description("The resource/operation you tried to access/perform does not exist.")
                .content(jsonContent(schema, example));
    }

    private ApiResponse badRequestApiResponse(String path) {
        var schema = new Schema<>();
        schema.$ref(ERROR_RESPONSE_SCHEMA_REF);
        var example = ErrorResponse.builder()
                .timestamp(LocalDateTime.now(clock))
                .code(400)
                .message("Bad Request")
                .path(path)
                .build();
        return new ApiResponse()
                .description("The request submitted is not valid. This might be due because the request does not pass the API validation.")
                .content(jsonContent(schema, example));
    }

    private ApiResponse internalServerErrorApiResponse(String path) {
        var schema = new Schema<>();
        schema.$ref(ERROR_RESPONSE_SCHEMA_REF);
        var example = ErrorResponse.builder()
                .timestamp(LocalDateTime.now(clock))
                .code(500)
                .message("Internal Server Error")
                .path(path)
                .build();
        return new ApiResponse()
                .description("An internal error occurred in the server.")
                .content(jsonContent(schema, example));
    }
}
