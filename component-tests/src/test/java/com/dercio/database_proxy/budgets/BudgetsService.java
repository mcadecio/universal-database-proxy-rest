package com.dercio.database_proxy.budgets;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

import static io.restassured.RestAssured.given;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BudgetsService {

    private final Mapper mapper;
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

    public Response createBudget(Budget budget) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(budget))
                .log()
                .all(true)
                .post(BUDGETS)
                .prettyPeek();
    }

    public Response deleteBudget(Long id) {
        return given()
                .baseUri(BASE_URI)
                .log()
                .all(true)
                .delete(BUDGETS + id)
                .prettyPeek();
    }

    public Response updateBudget(Long originalId, Budget budget) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(budget))
                .log()
                .all(true)
                .put(BUDGETS + originalId)
                .prettyPeek();
    }
}
