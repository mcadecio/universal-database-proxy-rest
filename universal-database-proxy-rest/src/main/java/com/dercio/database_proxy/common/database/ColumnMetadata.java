package com.dercio.database_proxy.common.database;

import com.dercio.database_proxy.postgres.PgTypeMapper;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ColumnMetadata {
    private final String tableSchema;
    private final String tableName;
    private final String columnName;
    private final String dataType;
    private final Long characterMaximumLength;
    private final Object columnDefault;
    private final boolean isNullable;

    public ColumnMetadata(JsonObject jsonObject) {
        this.tableSchema = jsonObject.getString("table_schema");
        this.tableName = jsonObject.getString("table_name");
        this.columnName = jsonObject.getString("column_name");
        this.dataType = PgTypeMapper.fromPgToSwagger(jsonObject.getString("data_type"));
        this.characterMaximumLength = jsonObject.getLong("character_maximum_length");
        this.columnDefault = jsonObject.getValue("column_default");
        this.isNullable = "YES".equals(jsonObject.getString("is_nullable"));
    }
}
