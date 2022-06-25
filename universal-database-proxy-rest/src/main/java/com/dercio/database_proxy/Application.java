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
                .map(config -> Guice.createInjector(new ApplicationModule(vertx, config)))
                .onSuccess(injector -> injector.getInstance(VerticleDeployer.class).process(injector))
                .onFailure(error -> {
                    log.fatal(error);
                    log.fatal("There was an error on the initial setup of the application.");
                    log.fatal("The application may have failed to retrive the configuration if so.");
                    log.fatal("Please create a file at {}", getConfigPath());
                    log.fatal("Or configure the '-Dproject.config' property and specify the config path.");
                    log.fatal("If it is none of the above correct your config.json and try again.");
                    log.fatal("Shutting down...");
                    vertx.close();
                });
    }

    private static ConfigRetrieverOptions configRetrieverOptions() {
        var configPath = getConfigPath();
        var configStore = new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", configPath));

        return new ConfigRetrieverOptions().addStore(configStore);
    }

    private static String getConfigPath() {
        return System.getProperty("project.config", "cfg/config.json");
    }

}
