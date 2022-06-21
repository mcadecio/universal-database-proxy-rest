package com.dercio.database_proxy.steps.budgets;

import com.dercio.database_proxy.common.DatabaseProxyService;
import com.dercio.database_proxy.common.ScenarioContext;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.repositories.budgets.Budget;
import com.dercio.database_proxy.repositories.budgets.BudgetsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.mybatis.guice.transactional.Transactional;

import java.util.HashSet;
import java.util.List;

import static com.dercio.database_proxy.steps.budgets.BudgetsFactory.createFebBudget;
import static com.dercio.database_proxy.steps.budgets.BudgetsFactory.createJanuaryBudget;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ScenarioScoped
public class GetBudgetsSteps {

    private final List<Budget> budgets;
    private final BudgetsRepository budgetsRepository;
    private final DatabaseProxyService databaseProxyService;
    private final Mapper mapper;
    private Response response;

    @Inject
    public GetBudgetsSteps(ScenarioContext scenarioContext,
                           BudgetsRepository budgetsRepository,
                           DatabaseProxyService databaseProxyService,
                           Mapper mapper) {
        this.budgetsRepository = budgetsRepository;
        this.databaseProxyService = databaseProxyService;
        this.budgets = scenarioContext.getBudgets();
        this.mapper = mapper;
    }

    @Transactional
    @Given("a list of budgets exists")
    public void aListOfBudgetsExists() {
        var januaryBudget = createJanuaryBudget();
        var febBudget = createFebBudget();

        budgets.add(januaryBudget);
        budgets.add(febBudget);

        budgetsRepository.save(januaryBudget);
        budgetsRepository.save(febBudget);
    }

    @When("I retrieve all the budgets")
    public void iRetrieveAllTheBudgets() {
        response = databaseProxyService.getBudgets();
    }

    @Then("I should see all the budgets")
    public void iShouldSeeAllTheBudgets() {
        assertEquals(200, response.statusCode());
        var budgetsResponse = mapper.decode(
                response.getBody().asString(),
                new TypeReference<List<Budget>>() {
                }
        );
        assertTrue(new HashSet<>(budgetsResponse).containsAll(budgets));
    }

    @When("I retrieve a budget with id {int}")
    public void iRetrieveABudgetWithId(int id) {
        response = databaseProxyService.getBudgetById(id);
    }

    @Then("I should see the budget")
    public void iShouldSeeTheBudget() {
        var budget = mapper.decode(response.body().asString(), new TypeReference<Budget>() {
        });

        assertTrue(budgets.contains(budget));
    }

    @Then("I should get a not found error message")
    public void iShouldGetANotFoundErrorMessage() {
        response.then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/5000"))
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }

    @When("I retrieve a budget with an invalid id")
    public void iRetrieveABudgetWithAnInvalidId() {
        response = databaseProxyService.getBudgetById("INVALID");
    }

    @Then("I should get a validation error message")
    public void iShouldGetAValidationErrorMessage() {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/INVALID"))
                .body("message", equalTo("[Bad Request] Parsing error for parameter id in location PATH: java.lang.NumberFormatException: For input string: \"INVALID\""))
                .body("code", equalTo(400));
    }

}
