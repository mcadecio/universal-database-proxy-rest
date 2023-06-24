package com.dercio.database_proxy.budgets.steps;

import com.dercio.database_proxy.budgets.BudgetsService;
import com.dercio.database_proxy.budgets.Budget;
import com.dercio.database_proxy.budgets.BudgetsRepository;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;

import static com.dercio.database_proxy.budgets.BudgetsFactory.createJanuaryBudget;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

@ScenarioScoped
public class DeleteBudgetsSteps {

    private final List<Budget> budgets;
    private final BudgetsRepository budgetsRepository;
    private final BudgetsService budgetsService;
    private Response response;

    @Inject
    public DeleteBudgetsSteps(BudgetsContext budgetsContext,
                              BudgetsRepository budgetsRepository,
                              BudgetsService budgetsService) {
        this.budgetsRepository = budgetsRepository;
        this.budgetsService = budgetsService;
        this.budgets = budgetsContext.getBudgets();
    }

    @Transactional
    @Given("the budget I previously created is no longer valid")
    public void theBudgetIPreviouslyCreatedIsNoLongerValid() {
        var januaryBudget = createJanuaryBudget();

        budgets.add(januaryBudget);

        budgetsRepository.save(januaryBudget);
    }

    @When("I delete the budget")
    public void iDeleteTheBudget() {
        response = budgetsService.deleteBudget(budgets.get(0).getId());
    }

    @Then("the budget should be deleted")
    public void theBudgetShouldBeDeleted() {
        response.then()
                .statusCode(204);
        assertNull(budgetsRepository.findById(budgets.get(0).getId()));
    }

    @When("I delete a budget that does not exist")
    public void iDeleteABudgetThatDoesNotExist() {
        response = budgetsService.deleteBudget(573489L);
    }

    @Then("I should be alerted that the budget does not exist")
    public void iShouldBeAlertedThatTheBudgetDoesNotExist() {
        response.then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/573489"))
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }

}
