package com.dercio.database_proxy.common;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class DatabaseProxyService {

    private static final String BASE_URI = "http://localhost:8000";
    private static final String BUDGETS = "/budgets/";

    public Response getBudgets() {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(BUDGETS)
                .prettyPeek();
    }

    public Response getBudgetById(long id) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(BUDGETS + id)
                .prettyPeek();
    }

    public Response getBudgetById(String id) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(BUDGETS + id)
                .prettyPeek();
    }
}
