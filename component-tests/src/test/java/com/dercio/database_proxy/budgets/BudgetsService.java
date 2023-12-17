package com.dercio.database_proxy.budgets;

import com.dercio.database_proxy.common.RestService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

import static io.restassured.RestAssured.given;

public class BudgetsService extends RestService {

    private static final String BASE_URI = "http://localhost:8000";
    private static final String BUDGETS = "/budgets/";

    @Inject
    public BudgetsService(Mapper mapper) {
        super(BASE_URI, BUDGETS, mapper);
    }

    public Response createBudget(Budget budget) {
        return create(budget);
    }

}
