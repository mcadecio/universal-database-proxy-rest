package com.dercio.database_proxy.common.verticle;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.google.inject.Injector;
import io.vertx.core.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import static org.reflections.scanners.Scanners.TypesAnnotated;

@Slf4j
@AllArgsConstructor
public class VerticleDeployer implements AnnotationProcessor<DeploymentOptions> {

    private final Vertx vertx;
    private final Injector injector;

    @Override
    public void process(DeploymentOptions deploymentOptions) {

        Handler<AsyncResult<String>> handler = result -> {
            if (result.failed()) {
                log.error("Failed to deploy verticle", result.cause());
                System.exit(500);
            }
        };

        new Reflections(basePackage()).get(TypesAnnotated.with(Verticle.class).asClass())
                .stream()
                .map(injector::getInstance)
                .filter(AbstractVerticle.class::isInstance)
                .map(AbstractVerticle.class::cast)
                .forEach(verticle -> vertx.deployVerticle(verticle, deploymentOptions, handler));
    }
}
