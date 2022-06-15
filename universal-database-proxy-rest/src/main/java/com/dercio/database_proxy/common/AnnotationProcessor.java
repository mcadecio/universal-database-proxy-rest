package com.dercio.database_proxy.common;

import com.dercio.database_proxy.Application;

@FunctionalInterface
public interface AnnotationProcessor<T> {

    void process(T argument);
    
    default String basePackage() {
        return Application.class.getPackageName();
    }
}
