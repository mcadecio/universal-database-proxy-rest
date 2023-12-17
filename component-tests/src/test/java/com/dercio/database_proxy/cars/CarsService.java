package com.dercio.database_proxy.cars;

import com.dercio.database_proxy.common.RestService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.restassured.response.Response;


public class CarsService extends RestService {

    private static final String BASE_URI = "http://localhost:8010";
    private static final String CARS = "/cars/";

    @Inject
    public CarsService(Mapper mapper) {
        super(BASE_URI, CARS, mapper);
    }

    public Response getCars() {
        return getAll();
    }

    public Response getCarById(String id) {
        return getById(id);
    }

    public Response getCarById(int id) {
        return getById(id);
    }

    public Response deleteCarById(int id) {
        return deleteById(id);
    }

    public Response updateCar(int id, Car car) {
        return update(id, car);
    }

    public Response createCar(Car car) {
        return create(car);
    }
}
