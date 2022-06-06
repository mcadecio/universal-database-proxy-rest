package com.dercio.database_proxy.postgres;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PgTypeMapper {
    public static final String STRING = "string";
    private static final String INTEGER = "integer";
    private static final String NUMERIC = "numeric";
    private static final String BIGINT = "bigint";
    private static final String DATE = "date";
    private static final String CHARACTER_VARYING = "character varying";
    private static final String BOOLEAN = "boolean";
    private static final String NUMBER = "number";
    private static final Map<String, String> PG_TYPE_TO_SWAGGER_TYPE = Map.of(
            INTEGER, INTEGER,
            NUMERIC, NUMBER,
            BIGINT, INTEGER,
            DATE, STRING,
            CHARACTER_VARYING, STRING,
            BOOLEAN, BOOLEAN
    );

    public static String fromPgToSwagger(String pgType) {
        return PG_TYPE_TO_SWAGGER_TYPE.get(pgType);
    }
}
