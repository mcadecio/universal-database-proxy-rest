package com.dercio.database_proxy.repositories.wheel;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

import static io.restassured.RestAssured.given;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WheelsService {

    private final Mapper mapper;
    private static final String BASE_URI = "http://localhost:8010";
    private static final String WHEEL = "/wheel/";

    public Response getWheels() {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(WHEEL)
                .prettyPeek();
    }

    public Response getWheelByType(String name) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(WHEEL + name)
                .prettyPeek();
    }

    public Response getWheelByType(int id) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(WHEEL + id)
                .prettyPeek();
    }

    public Response deleteWheelByType(String name) {
        return given()
                .baseUri(BASE_URI)
                .log()
                .all(true)
                .delete(WHEEL + name)
                .prettyPeek();
    }

    public Response updateWheel(String originalWheelType, Wheel wheel) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(wheel))
                .log()
                .all(true)
                .put(WHEEL + originalWheelType)
                .prettyPeek();
    }

    public Response createWheel(Wheel wheel) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(wheel))
                .log()
                .all(true)
                .post(WHEEL)
                .prettyPeek();
    }
}
