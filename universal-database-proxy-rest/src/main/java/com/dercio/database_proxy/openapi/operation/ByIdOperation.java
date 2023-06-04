package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.List;

public abstract class ByIdOperation extends OpenApiOperation {

    @Override
    protected List<Parameter> operationParameters(TableMetadata tableMetadata) {
        var primaryKeyColumn = tableMetadata.getPrimaryKeyColumn();
        var parameter = new Parameter()
                .name(primaryKeyColumn.getColumnName())
                .in("path")
                .required(true)
                .schema(new ObjectSchema().type(primaryKeyColumn.getOpenApiType()));
        return List.of(parameter);
    }
}
