package com.dercio.database_proxy.common.verticle;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.dercio.database_proxy.common.exceptions.VerticleDisabledException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reflections.Reflections;

import java.util.Set;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VerticleDeployer implements AnnotationProcessor<Injector> {

    private final Vertx vertx;
    private final Set<AbstractVerticle> verticles;

    @Override
    public void process(Injector injector) {

        Handler<AsyncResult<String>> handler = result -> {
            if (result.failed()) {
                if (!(result.cause() instanceof VerticleDisabledException))
                    log.error(result.cause().getMessage(), result.cause());
                else
                    log.info(result.cause().getMessage());

            }
        };

        new Reflections(basePackage())
                .getTypesAnnotatedWith(Verticle.class)
                .stream()
                .map(injector::getInstance)
                .filter(AbstractVerticle.class::isInstance)
                .map(AbstractVerticle.class::cast)
                .forEach(verticle -> vertx.deployVerticle(verticle, handler));

        verticles.forEach(verticle -> vertx.deployVerticle(verticle, handler));
    }
}
