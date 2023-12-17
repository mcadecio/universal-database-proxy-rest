package com.dercio.database_proxy.openapi;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.openapi.operation.*;
import com.google.inject.Inject;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class OpenApiPathsCreator {

    private final GetOperation getOperation;
    private final PostOperation postOperation;
    private final GetByIdOperation getByIdOperation;
    private final PutByIdOperation putByIdOperation;
    private final DeleteByIdOperation deleteByIdOperation;

    public Paths createPaths(List<TableMetadata> tables) {
        Paths paths = new Paths();

        tables.forEach(tableMetadata -> {
            PathItem pathItem = new PathItem()
                    .get(getOperation.createOperation(tableMetadata))
                    .post(postOperation.createOperation(tableMetadata));

            PathItem pathItemById = new PathItem()
                    .get(getByIdOperation.createOperation(tableMetadata))
                    .put(putByIdOperation.createOperation(tableMetadata))
                    .delete(deleteByIdOperation.createOperation(tableMetadata));

            paths.addPathItem("/" + tableMetadata.getTableName(), pathItem);

            String idParams = tableMetadata.getColumns()
                    .stream()
                    .filter(column -> column.isPrimaryKey())
                    .map(column -> "{" + column.getColumnName() + "}")
                    .collect(Collectors.joining(":"));
            paths.addPathItem(String.format("/%s/%s", tableMetadata.getTableName(), idParams), pathItemById);
        });

        return paths;
    }

}
