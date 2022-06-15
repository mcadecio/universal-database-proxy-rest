package com.dercio.database_proxy.common.exceptions;

public class VerticleDisabledException extends RuntimeException {

    public VerticleDisabledException(String verticleName) {
        super("Verticle named " + verticleName + " is disabled");
    }
}
