package com.dercio.database_proxy.common.module;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.google.inject.AbstractModule;
import lombok.SneakyThrows;
import org.reflections.Reflections;

import java.util.function.Consumer;

public class ModuleInstaller implements AnnotationProcessor<Consumer<AbstractModule>> {

    @Override
    public void process(Consumer<AbstractModule> install) {
        new Reflections(basePackage())
                .getTypesAnnotatedWith(Module.class)
                .stream()
                .map(this::createInstance)
                .filter(AbstractModule.class::isInstance)
                .map(AbstractModule.class::cast)
                .forEach(install);
    }

    @SneakyThrows
    private Object createInstance(Class<?> aClass) {
        return aClass.getDeclaredConstructor().newInstance();
    }
}
