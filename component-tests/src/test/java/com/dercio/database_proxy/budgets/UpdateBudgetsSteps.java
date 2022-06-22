package com.dercio.database_proxy.budgets;

import com.dercio.database_proxy.repositories.budgets.BudgetsService;
import com.dercio.database_proxy.repositories.budgets.Budget;
import com.dercio.database_proxy.repositories.budgets.BudgetsRepository;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;

import static com.dercio.database_proxy.budgets.BudgetsFactory.createJanuaryBudget;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ScenarioScoped
public class UpdateBudgetsSteps {

    private final List<Budget> budgets;
    private final BudgetsRepository budgetsRepository;
    private final BudgetsService budgetsService;
    private Response response;

    @Inject
    public UpdateBudgetsSteps(BudgetsContext budgetsContext,
                              BudgetsRepository budgetsRepository,
                              BudgetsService budgetsService) {
        this.budgetsRepository = budgetsRepository;
        this.budgetsService = budgetsService;
        this.budgets = budgetsContext.getBudgets();
    }

    @Given("a budget exists")
    @Transactional
    public void aBudgetExists() {
        var januaryBudget = createJanuaryBudget();

        budgets.add(januaryBudget);

        budgetsRepository.save(januaryBudget);
    }

    @When("I update the id of the budget")
    public void iUpdateTheIdOfTheBudget() {
        budgets.add(createJanuaryBudget());
        var budget = budgets.get(0);
        var originalId = budget.getId();
        budget.setId(999L);
        response = budgetsService.updateBudget(originalId, budget);
    }

    @Then("I should be alerted that id cannot be updated")
    public void iShouldBeAlertedThatIdCannotBeUpdated() {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/2000"))
                .body("message", containsString("inconsistent primary key values"))
                .body("code", equalTo(400));
    }

    @When("I update the month to no value")
    public void iUpdateTheMonthToNoValue() {
        var budget = budgets.get(0);

        budget.setMonth(null);

        response = budgetsService.updateBudget(budget.getId(), budget);
    }

    @Then("I should be alerted that the month is a required field")
    public void iShouldBeAlertedThatTheMonthIsARequiredField() {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/2000"))
                .body("message", equalTo("month cannot be null"))
                .body("code", equalTo(400));
    }

    @When("I update the user id field")
    public void iUpdateTheUserIdField() {
        var budget = budgets.get(0);

        budget.setUserId("something-else");

        response = budgetsService.updateBudget(budget.getId(), budget);
    }

    @Then("I should see the new user id in the budget")
    public void iShouldSeeTheNewUserIdInTheBudget() {
        response.then()
                .statusCode(204);

        var budget = budgets.get(0);
        var actualBudget = budgetsRepository.findById(budget.getId());
        assertEquals("something-else", actualBudget.getUserId());
        assertEquals(budget, actualBudget);
    }

    @When("I update the year field")
    public void iUpdateTheYearField() {
        var budget = budgets.get(0);

        budget.setYear(1999);

        response = budgetsService.updateBudget(budget.getId(), budget);
    }

    @Then("I should see the new year in the budget")
    public void iShouldSeeTheNewYearInTheBudget() {
        response.then()
                .statusCode(204);

        var budget = budgets.get(0);
        var actualBudget = budgetsRepository.findById(budget.getId());
        assertEquals(1999, actualBudget.getYear());
        assertEquals(budget, actualBudget);
    }

    @When("I update a budget that does not exist")
    public void iUpdateABudgetThatDoesNotExist() {
        var januaryBudget = createJanuaryBudget();

        response = budgetsService.updateBudget(januaryBudget.getId(), januaryBudget);
    }

    @Then("I should notified the budget does not exist")
    public void iShouldNotifiedTheBudgetDoesNotExist() {
        response.then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/2000"))
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }
}
