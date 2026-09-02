package com.dercio.database_proxy.common.database;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.function.Function;

@ToString
@Getter
public class ColumnMetadata {
    private final String tableSchema;
    private final String tableName;
    private final String columnName;
    private final String openApiType;
    private final String openApiItemsType;
    private final String dbType;
    private final Long characterMaximumLength;
    private final Object columnDefault;
    private final boolean isNullable;
    @Setter
    private boolean isPrimaryKey;

    /**
     * @param openApiTypeResolver maps this engine's {@code data_type} spelling to an
     *                            {@link com.dercio.database_proxy.openapi.OpenApiType}. Engines name
     *                            their types differently, so each supplies its own resolver.
     */
    public ColumnMetadata(JsonObject jsonObject, Function<String, OpenApiColumnType> openApiTypeResolver) {
        this.tableSchema = jsonObject.getString("table_schema");
        this.tableName = jsonObject.getString("table_name");
        this.columnName = jsonObject.getString("column_name");
        this.dbType = jsonObject.getString("data_type");

        var resolvedType = openApiTypeResolver.apply(dbType);
        this.openApiType = resolvedType.type();
        this.openApiItemsType = resolvedType.itemsType();

        this.characterMaximumLength = jsonObject.getLong("character_maximum_length");
        this.columnDefault = jsonObject.getValue("column_default");
        this.isNullable = "YES".equals(jsonObject.getString("is_nullable"));
        this.isPrimaryKey = jsonObject.getBoolean("is_primary_key", false);
    }
}
