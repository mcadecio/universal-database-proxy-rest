package com.dercio.database_proxy.common.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class TableMetadata {
    private final List<ColumnMetadata> columns;
    private final String databaseName;
    private final String schemaName;
    private final String tableName;
    private String pkColumnName;
}
