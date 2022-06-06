package com.dercio.database_proxy;

import com.dercio.database_proxy.common.verticle.VerticleDeployer;
import com.google.inject.Guice;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Application {

    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        ConfigRetriever.create(vertx, configRetrieverOptions())
                .getConfig()
                .onSuccess(config -> {
                    var injector = Guice.createInjector(new ApplicationModule(vertx, config));
                    injector.getInstance(VerticleDeployer.class).process(injector);
                });
    }

    private static ConfigRetrieverOptions configRetrieverOptions() {
        var configPath = System.getProperty("project.config", "cfg/config.json");
        var configStore = new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", configPath));

        return new ConfigRetrieverOptions().addStore(configStore);
    }

}
