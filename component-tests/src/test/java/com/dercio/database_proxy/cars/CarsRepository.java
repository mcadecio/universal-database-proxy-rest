package com.dercio.database_proxy.cars;


public interface CarsRepository {
    Car findById(int id);

    void save(Car car);

    void deleteById(int id);
}
