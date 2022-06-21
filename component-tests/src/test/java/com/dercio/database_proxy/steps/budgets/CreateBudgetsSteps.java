package com.dercio.database_proxy.steps.budgets;

import com.dercio.database_proxy.common.DatabaseProxyService;
import com.dercio.database_proxy.common.ScenarioContext;
import com.dercio.database_proxy.repositories.budgets.Budget;
import com.dercio.database_proxy.repositories.budgets.BudgetsRepository;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;

import static com.dercio.database_proxy.steps.budgets.BudgetsFactory.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ScenarioScoped
public class CreateBudgetsSteps {

    private final List<Budget> budgets;
    private final BudgetsRepository budgetsRepository;
    private final DatabaseProxyService databaseProxyService;
    private Response response;

    @Inject
    public CreateBudgetsSteps(ScenarioContext scenarioContext,
                              BudgetsRepository budgetsRepository,
                              DatabaseProxyService databaseProxyService) {
        this.budgetsRepository = budgetsRepository;
        this.databaseProxyService = databaseProxyService;
        this.budgets = scenarioContext.getBudgets();
    }

    @When("I create a budget with all the fields")
    public void iCreateABudgetWithAllTheFields() {
        var budget = createJanuaryBudget();
        budgets.add(budget);
        response = databaseProxyService.createBudget(budget);
    }

    @Then("I should get a link to the budget")
    public void iShouldGetALinkToTheBudget() {
        response.then()
                .statusCode(201)
                .header("Location", "http://localhost:8000/budgets/" + budgets.get(0).getId());
        assertEquals(budgets.get(0), budgetsRepository.findById(budgets.get(0).getId()));
    }

    @Given("the budget I am trying to create already exists")
    public void theBudgetIAmTryingToCreateAlreadyExists() {
        var budget = createJanuaryBudget();
        budgets.add(budget);
        budgetsRepository.save(budget);
    }

    @When("I create the same budget")
    public void iCreateTheSameBudget() {
        response = databaseProxyService.createBudget(budgets.get(0));
    }

    @Then("I should get an error message")
    public void iShouldGetAnErrorMessage() {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/"))
                .body("message", equalTo("ERROR: duplicate key value violates unique constraint \"budgets_pkey\" (23505)"))
                .body("code", equalTo(400));
    }

    @When("I create a budget with the required fields only")
    public void iCreateABudgetWithTheRequiredFieldsOnly() {
        var budget = createRequiredBudget();
        budgets.add(budget);
        response = databaseProxyService.createBudget(budget);
    }

    @When("I create a budget with the optional fields only")
    public void iCreateABudgetWithTheOptionalFieldsOnly() {
        var budget = createOptionalBudget();
        budgets.add(budget);
        response = databaseProxyService.createBudget(budget);
    }

    @Then("I should get a the following validation error message: {string}")
    public void iShouldGetAValidationErrorMessageWithTheFollowingMessage(String message) {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/"))
                .body("message", equalTo(message))
                .body("code", equalTo(400));
    }

    @When("I create a budget with an incorrect value for a field")
    public void iCreateABudgetWithAnIncorrectValueForAField() {
        response = databaseProxyService.createBudget(createBudgetWithInvalidFieldType());
    }
}
