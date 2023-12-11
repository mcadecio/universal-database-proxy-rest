package com.dercio.database_proxy.common.database;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TableRequest {
    private String database;
    private String schema;
    private String table;
    private Map<String, String> queryParams;
    private Map<String, String> pathParams;
    private JsonObject body;
}
