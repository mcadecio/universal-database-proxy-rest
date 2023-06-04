package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import static com.simplaex.http.StatusCode._200;

public class GetByIdOperation extends ByIdOperation {

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

        MediaType mediaType = new MediaType();
        mediaType.setSchema(objectSchema);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A JSON representing " + tableMetadata.getTableName());
        apiResponse.setContent(jsonContent(objectSchema));

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse(String.valueOf(_200.getCode()), apiResponse);

        return apiResponses;
    }

}
