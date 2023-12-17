package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.google.inject.Inject;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.time.Clock;
import java.util.List;

import static com.simplaex.http.StatusCode._204;

public class DeleteOperation extends OpenApiOperation {

    @Inject
    public DeleteOperation(Clock clock) {
        super(clock);
    }

    @Override
    protected String operationSummary(TableMetadata tableMetadata) {
        return String.format("Deletes %s", tableMetadata.getTableName());
    }

    @Override
    protected String operationId(TableMetadata tableMetadata) {
        return "delete_" + tableMetadata.getTableName();
    }

    @Override
    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        ApiResponses apiResponses = super.operationApiResponses(tableMetadata);
        apiResponses.addApiResponse(String.valueOf(_204.getCode()), new ApiResponse()
                .description("Deleted rows from " + tableMetadata.getTableName() + " successfully."));
        return apiResponses;
    }

    @Override
    protected List<Parameter> operationParameters(TableMetadata tableMetadata) {
        return tableMetadata.getColumns()
                .stream()
                .map(column -> new Parameter()
                        .name(column.getColumnName())
                        .in("query")
                        .schema(schemaFromColumn(column)))
                .toList();
    }
}
