package com.dercio.database_proxy.openapi.operation;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.google.inject.Inject;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.time.Clock;

import static com.simplaex.http.StatusCode.*;

public class PostOperation extends OpenApiOperation {

    @Inject
    public PostOperation(Clock clock) {
        super(clock);
    }

    @Override
    protected String operationSummary(TableMetadata tableMetadata) {
        return "Creates a new " + tableMetadata.getTableName();
    }

    @Override
    protected String operationId(TableMetadata tableMetadata) {
        return "create_" + tableMetadata.getTableName();
    }

    @Override
    protected ApiResponses operationApiResponses(TableMetadata tableMetadata) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("A new " + tableMetadata.getTableName() + " has been created.");
        Header locationHeader = new Header()
                .description("The URI of the resource created.")
                .schema(new StringSchema());
        apiResponse.addHeaderObject("Location", locationHeader);

        ApiResponses apiResponses = super.operationApiResponses(tableMetadata);
        apiResponses.addApiResponse(String.valueOf(_201.getCode()), apiResponse);
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
