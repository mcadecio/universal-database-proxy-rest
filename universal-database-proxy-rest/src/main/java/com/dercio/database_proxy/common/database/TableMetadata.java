package com.dercio.database_proxy.common.database;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Log4j2
public class TableMetadata {
    private final List<ColumnMetadata> columns;
    private final String databaseName;
    private final String schemaName;
    private final String tableName;
    private String pkColumnName;

    public ColumnMetadata getPrimaryKeyColumn() {
        return columns
                .stream()
                .filter(column -> column.getColumnName().equals(pkColumnName))
                .findAny()
                .orElseThrow(() -> {
                    var message = "Unable to find a column that matches the table PK column name";
                    log.error(message);
                    log.error("Table: {}", JsonObject.mapFrom(this).encodePrettily());
                    return new IllegalStateException(message);
                });
    }

    public List<String> getColumnNames() {
        return columns.stream()
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.toList());
    }

    public List<ColumnMetadata> getNonPrimaryKeyColumns() {
        return columns
                .stream()
                .filter(column -> !column.getColumnName().equals(pkColumnName))
                .collect(Collectors.toList());
    }
}
