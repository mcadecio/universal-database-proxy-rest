package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import static com.simplaex.http.StatusCode._200;

public class DeleteByIdOperation extends ByIdOperation {

    @Override
    protected String operationSummary(TableMetadata tableMetadata) {
        return "Delete " + tableMetadata.getTableName() + " by id";
    }

    @Override
    protected String operationId(TableMetadata tableMetadata) {
        return "delete_" + tableMetadata.getTableName() + "_by_id";
    }

    @Override
    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), new ApiResponse()
                .description("Deleted " + tableMetadata.getTableName() + " successfully."));
        return apiResponses;
    }
}
