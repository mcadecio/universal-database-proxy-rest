package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.List;
import java.util.stream.Collectors;

import static com.simplaex.http.StatusCode._200;

public class GetOperation extends OpenApiOperation {

    @Override
    protected String operationSummary(TableMetadata tableMetadata) {
        return "Returns a list of " + tableMetadata.getTableName();
    }

    @Override
    protected String operationId(TableMetadata tableMetadata) {
        return "get_" + tableMetadata.getTableName();
    }

    @Override
    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        Schema<Object> objectSchema = createSchemaFromColumns(tableMetadata.getColumns());
        ArraySchema schema = new ArraySchema().items(objectSchema);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON array of " + tableMetadata.getTableName());
        apiResponse.setContent(jsonContent(schema));

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        return apiResponses;
    }

    @Override
    protected List<Parameter> operationParameters(TableMetadata tableMetadata) {
        return tableMetadata.getColumns()
                .stream()
                .map(this::columnToQueryParameter)
                .collect(Collectors.toList());
    }

    private Parameter columnToQueryParameter(ColumnMetadata column) {
        return new Parameter()
                .name(column.getColumnName())
                .in("query")
                .schema(schemaFromColumn(column));
    }
}
