package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.google.inject.Inject;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.time.Clock;

import static com.simplaex.http.StatusCode._200;

public class DeleteByIdOperation extends ByIdOperation {

    @Inject
    public DeleteByIdOperation(Clock clock) {
        super(clock);
    }

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
        ApiResponses apiResponses = super.operationApiResponses(tableMetadata);
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), new ApiResponse()
                .description("Deleted " + tableMetadata.getTableName() + " successfully."));
        return apiResponses;
    }
}
