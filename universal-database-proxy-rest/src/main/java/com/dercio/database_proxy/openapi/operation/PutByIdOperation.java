package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import static com.simplaex.http.StatusCode._204;

public class PutByIdOperation extends ByIdOperation {
    @Override
    protected String operationSummary(TableMetadata tableMetadata) {
        return "Update a " + tableMetadata.getTableName() + " by id";
    }

    @Override
    protected String operationId(TableMetadata tableMetadata) {
        return "update_" + tableMetadata.getTableName() + "_by_id";
    }

    @Override
    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("The " + tableMetadata.getTableName() + " has been updated.");

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_204.getCode()), apiResponse);
        return apiResponses;
    }

    @Override
    protected RequestBody operationRequestBody(TableMetadata tableMetadata) {
        var objectSchema = createSchemaFromColumns(tableMetadata.getColumns());
        return new RequestBody()
                .required(true)
                .content(jsonContent(objectSchema));

    }
}
