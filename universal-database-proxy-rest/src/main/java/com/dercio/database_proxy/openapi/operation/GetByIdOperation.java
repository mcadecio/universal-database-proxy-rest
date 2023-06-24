package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.google.inject.Inject;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.time.Clock;

import static com.simplaex.http.StatusCode._200;

public class GetByIdOperation extends ByIdOperation {

    @Inject
    public GetByIdOperation(Clock clock) {
        super(clock);
    }

    @Override
    protected String operationSummary(TableMetadata tableMetadata) {
        return "Get a " + tableMetadata.getTableName() + " by id";
    }

    @Override
    protected String operationId(TableMetadata tableMetadata) {
        return "get_" + tableMetadata.getTableName() + "_by_id";
    }

    @Override
    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        var objectSchema = createSchemaFromColumns(tableMetadata.getColumns());

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON representing " + tableMetadata.getTableName());
        apiResponse.setContent(jsonContent(objectSchema));

        ApiResponses apiResponses = super.operationApiResponses(tableMetadata);
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        return apiResponses;
    }

}
