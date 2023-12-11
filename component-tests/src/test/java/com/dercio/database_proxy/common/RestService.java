package com.dercio.database_proxy.common;

import com.dercio.database_proxy.common.mapper.Mapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestService {
    private final String baseUri;
    private final String path;
    private final Mapper mapper;

    public RestService(String baseUri, String path, Mapper mapper) {
        this.baseUri = baseUri;
        this.path = path;
        this.mapper = mapper;
    }

    public Response getAll() {
        return given()
                .baseUri(baseUri)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(path)
                .prettyPeek();
    }

    public Response getAll(Map<String, Object> filters) {
        return given()
                .baseUri(baseUri)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .queryParams(filters)
                .get(path)
                .prettyPeek();
    }

    public Response getById(Object id) {
        return given()
                .baseUri(baseUri)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(path + id)
                .prettyPeek();
    }

    public Response getById(int id) {
        return given()
                .baseUri(baseUri)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(path + id)
                .prettyPeek();
    }

    public Response getById(Object firstValue, Object secondValue) {
        return given()
                .urlEncodingEnabled(false)
                .when()
                .baseUri(baseUri)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(path + firstValue + ":" + secondValue)
                .prettyPeek();
    }

    public Response deleteById(int id) {
        return given()
                .baseUri(baseUri)
                .log()
                .all(true)
                .delete(path + id)
                .prettyPeek();
    }

    public Response deleteById(Object id) {
        return given()
                .baseUri(baseUri)
                .log()
                .all(true)
                .delete(path + id)
                .prettyPeek();
    }

    public Response deleteById(Object firstValue, Object secondValue) {
        return given()
                .urlEncodingEnabled(false)
                .when()
                .baseUri(baseUri)
                .log()
                .all(true)
                .delete(path + firstValue + ":" + secondValue)
                .prettyPeek();
    }

    public Response update(int id, Object resorce) {
        return given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON)
                .body(mapper.encode(resorce))
                .log()
                .all(true)
                .put(path + id)
                .prettyPeek();
    }

    public Response update(Object firstId, Object secondId, Object resource) {
        return given()
                .urlEncodingEnabled(false)
                .baseUri(baseUri)
                .contentType(ContentType.JSON)
                .body(mapper.encode(resource))
                .log()
                .all(true)
                .put(path + firstId + ":" + secondId)
                .prettyPeek();
    }

    public Response create(Object resource) {
        return given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON)
                .body(mapper.encode(resource))
                .log()
                .all(true)
                .post(path)
                .prettyPeek();
    }
}
