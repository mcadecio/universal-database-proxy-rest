package com.dercio.database_proxy;

import com.dercio.database_proxy.common.codec.CodecRegister;
import com.dercio.database_proxy.common.configuration.ConfigurationBinder;
import com.dercio.database_proxy.common.module.ModuleInstaller;
import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class ApplicationModule extends AbstractModule {
    private final Vertx vertx;
    private final JsonObject config;

    @Override
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);

        new CodecRegister().process(vertx.eventBus());
        new ModuleInstaller().process(this::install);
        new ConfigurationBinder(this::bind).process(config);
    }

}
