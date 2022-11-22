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
    private final String openApiType;
    private final String dbType;
    private final Long characterMaximumLength;
    private final Object columnDefault;
    private final boolean isNullable;
    private final Integer ordinalPosition;

    public ColumnMetadata(JsonObject jsonObject) {
        this.tableSchema = jsonObject.getString("table_schema");
        this.tableName = jsonObject.getString("table_name");
        this.columnName = jsonObject.getString("column_name");
        this.dbType = jsonObject.getString("data_type");
        this.openApiType = PgTypeMapper.fromPgToSwagger(dbType);
        this.characterMaximumLength = jsonObject.getLong("character_maximum_length");
        this.columnDefault = jsonObject.getValue("column_default");
        this.isNullable = "YES".equals(jsonObject.getString("is_nullable"));
        this.ordinalPosition = jsonObject.getInteger("ordinal_position");
    }
}
