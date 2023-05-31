package com.dercio.database_proxy.wheel;

public interface WheelsRepository {
    Wheel findByType(String type);

    void save(Wheel wheel);

    void deleteByType(String type);
}
