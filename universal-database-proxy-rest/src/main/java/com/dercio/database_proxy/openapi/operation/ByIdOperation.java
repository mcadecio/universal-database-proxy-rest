package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.time.Clock;
import java.util.List;

public abstract class ByIdOperation extends OpenApiOperation {

    protected ByIdOperation(Clock clock) {
        super(clock);
    }

    @Override
    protected List<Parameter> operationParameters(TableMetadata tableMetadata) {
        return tableMetadata.getColumns()
                .stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .map(column -> createParameter(true, column.getColumnName(), pathParameterType(column)))
                .toList();
    }

    /**
     * A frozen collection can legally be part of a primary key, and {@code array} without
     * {@code items} is an invalid schema, so fall back to the element type.
     */
    private String pathParameterType(ColumnMetadata column) {
        return column.getOpenApiItemsType() == null
                ? column.getOpenApiType()
                : column.getOpenApiItemsType();
    }

    private Parameter createParameter(boolean isPathParameter, String name, String type) {
        return new Parameter()
                .name(name)
                .in(isPathParameter ? "path" : "query")
                .required(true)
                .schema(new ObjectSchema().type(type));
    }
}
