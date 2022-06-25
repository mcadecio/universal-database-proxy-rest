package com.dercio.database_proxy.repositories.cars;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.repositories.football.NationalFootballTeam;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

import static io.restassured.RestAssured.given;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CarsService {

    private final Mapper mapper;
    private static final String BASE_URI = "http://localhost:8010";
    private static final String CARS = "/cars/";

    public Response getCars() {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(CARS)
                .prettyPeek();
    }

    public Response getCarById(String id) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(CARS + id)
                .prettyPeek();
    }

    public Response getCarById(int id) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(CARS + id)
                .prettyPeek();
    }

    public Response deleteCarById(int id) {
        return given()
                .baseUri(BASE_URI)
                .log()
                .all(true)
                .delete(CARS + id)
                .prettyPeek();
    }

    public Response updateCar(int id, Car car) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(car))
                .log()
                .all(true)
                .put(CARS + id)
                .prettyPeek();
    }

    public Response createCar(Car car) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(car))
                .log()
                .all(true)
                .post(CARS)
                .prettyPeek();
    }
}
