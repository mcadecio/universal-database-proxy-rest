package com.dercio.database_proxy;

import com.dercio.database_proxy.common.codec.CodecRegister;
import com.dercio.database_proxy.common.module.ModuleInstaller;
import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationModule extends AbstractModule {
    private final Vertx vertx;

    public ApplicationModule(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);

        new CodecRegister().process(vertx.eventBus());
        new ModuleInstaller().process(this::install);
    }

}
