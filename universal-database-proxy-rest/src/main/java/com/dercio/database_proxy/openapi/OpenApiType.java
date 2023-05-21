package com.dercio.database_proxy.openapi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OpenApiType {

    public static final String INTEGER = "integer";
    public static final String NUMBER = "number";
    public static final String BOOLEAN = "boolean";
    public static final String STRING = "string";
    public static final String OBJECT = "object";
    public static final String ANY = "ANY";
}
