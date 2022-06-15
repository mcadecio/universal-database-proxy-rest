package com.dercio.database_proxy.common.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class Table {
    private final List<TableColumn> columns;
    private final String databaseName;
    private final String schemaName;
    private final String tableName;
    private String pkColumnName;
}
