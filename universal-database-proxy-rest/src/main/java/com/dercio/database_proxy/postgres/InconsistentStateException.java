package com.dercio.database_proxy.postgres;

public class InconsistentStateException extends RuntimeException {

    public static final String INCONSISTENT_PRIMARY_KEY_VALUES = "The resource you are trying to update contains inconsistent primary key values. If you are trying to update the PK value you cannot do that.";

    public InconsistentStateException() {
        super(INCONSISTENT_PRIMARY_KEY_VALUES);
    }
}
