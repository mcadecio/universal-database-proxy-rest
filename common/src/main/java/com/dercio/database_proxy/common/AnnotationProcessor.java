package com.dercio.database_proxy.common;

@FunctionalInterface
public interface AnnotationProcessor<T> {

    void process(T argument);
    
    default String basePackage() {
        return "com.dercio.database_proxy";
    }
}
