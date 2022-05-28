package com.dercio.database_proxy.common.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class ConsumerVerticle extends AbstractVerticle {

    public abstract String getAddress();

    protected void logRegistration(Promise<Void> promise, AsyncResult<Void> result) {
        if (result.succeeded()) {
            log.info("Registered -{}- consumer", getAddress());
        } else {
            log.info("Failed to register -{}- consumer", getAddress());
        }
        promise.complete();
    }

    protected void logUnregistration(Promise<Void> promise, AsyncResult<Void> result) {
        if (result.succeeded()) {
            log.info("Unregistered -{}- consumer", getAddress());
        } else {
            log.info("Failed to unregister -{}- consumer", getAddress());
        }
        promise.complete();
    }
}
