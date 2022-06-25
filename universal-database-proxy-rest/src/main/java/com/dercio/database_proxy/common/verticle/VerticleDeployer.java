package com.dercio.database_proxy.common.verticle;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.dercio.database_proxy.common.exceptions.VerticleDisabledException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.vertx.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VerticleDeployer implements AnnotationProcessor<Injector> {

    private final Vertx vertx;
    private final Set<AbstractVerticle> verticles;

    @Override
    public void process(Injector injector) {

        Handler<Throwable> failureHandler = error -> {
            if (error instanceof VerticleDisabledException)
                log.info(error.getMessage());
            else
                log.error(error.getMessage(), error);
        };

        var annotatedVerticles = new Reflections(basePackage())
                .getTypesAnnotatedWith(Verticle.class)
                .stream()
                .map(injector::getInstance)
                .filter(AbstractVerticle.class::isInstance)
                .map(AbstractVerticle.class::cast)
                .collect(Collectors.toList());

        List<Future> deploymentFutures = Stream.of(annotatedVerticles, verticles)
                .flatMap(Collection::stream)
                .map(vertx::deployVerticle)
                .map(deploymentFuture -> deploymentFuture.onFailure(failureHandler))
                .collect(Collectors.toList());

        CompositeFuture.any(deploymentFutures)
                .onSuccess(future -> log.info("Deployment is complete"))
                .onFailure(error -> log.info("No verticles were deployed. Shutting down..."))
                .onFailure(error -> vertx.close());
    }
}
