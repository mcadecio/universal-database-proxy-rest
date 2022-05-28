package com.dercio.database_proxy;

import com.dercio.database_proxy.common.codec.CodecRegister;
import com.dercio.database_proxy.common.module.ModuleInstaller;
import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import lombok.extern.log4j.Log4j2;

@Log4j2
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
