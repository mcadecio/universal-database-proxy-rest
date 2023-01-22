package com.dercio.database_proxy.common.database;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@Log4j2
public class TableMetadata {
    private final String databaseName;
    private final String schemaName;
    private final String tableName;
    private ColumnMetadata primaryKeyColumn;
    private final List<ColumnMetadata> columns;

    public TableMetadata(String databaseName, String schemaName, String tableName, List<ColumnMetadata> columns) {
        this.databaseName = databaseName;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columns = columns;
        setPrimaryKeyColumn();
    }

    public List<String> getColumnNames() {
        return columns.stream()
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.toList());
    }

    public List<ColumnMetadata> getNonPrimaryKeyColumns() {
        return columns
                .stream()
                .filter(column -> !column.getColumnName().equals(getPkColumnName()))
                .collect(Collectors.toList());
    }

    public String getPkColumnName() {
        return primaryKeyColumn.getColumnName();
    }

    public int getNumberOfColumns() {
        return columns.size();
    }

    private void setPrimaryKeyColumn() {
        this.primaryKeyColumn = columns
                .stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .findFirst()
                .orElseGet(() -> {
                    var column = columns.get(0);

                    log.debug("Table [{}] does not have a PK. Using [{}] column as PK",
                            tableName,
                            column.getColumnName()
                    );

                    return column;
                });
    }
}
