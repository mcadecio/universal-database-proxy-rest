package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.openapi.OpenApiType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class OpenApiOperation {

    private static final String AUTO_GENERATED = "Auto Generated";

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

    protected abstract ApiResponses operationApiResponses(TableMetadata tableMetadata);

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

}
