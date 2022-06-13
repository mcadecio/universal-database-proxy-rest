package com.dercio.database_proxy.common.codec;

import com.dercio.database_proxy.common.AnnotationProcessor;
import io.vertx.core.eventbus.EventBus;
import org.reflections.Reflections;

public class CodecRegister implements AnnotationProcessor<EventBus> {

    @Override
    public void process(EventBus eventBus) {
        new Reflections(basePackage())
                .getTypesAnnotatedWith(Codec.class)
                .stream()
                .map(aClass -> (Class<Object>) aClass)
                .forEach(aClass -> eventBus.registerDefaultCodec(aClass, new GenericCodec<>(aClass)));
    }
}
