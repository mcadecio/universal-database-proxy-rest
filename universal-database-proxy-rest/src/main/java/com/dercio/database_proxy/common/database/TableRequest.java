package com.dercio.database_proxy.common.database;

import io.vertx.core.MultiMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TableRequest {
    private String database;
    private String schema;
    private String table;
    private MultiMap queryFilters;
}
