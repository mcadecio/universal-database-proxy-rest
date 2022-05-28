package com.dercio.database_proxy;

import com.dercio.database_proxy.common.verticle.VerticleDeployer;
import com.google.inject.Guice;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Application {

    public static void main(String[] args) {
        var vertx = Vertx.vertx();
        var deployment = new DeploymentOptions();
        var injector = Guice.createInjector(new ApplicationModule(vertx));
        new VerticleDeployer(vertx, injector).process(deployment);
    }

}
