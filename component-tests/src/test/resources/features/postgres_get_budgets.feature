@postgres
Feature: Get Budgets

  Scenario: A user can retrieve a list of budgets
    Given a list of budgets exists
    When I retrieve all the budgets
    Then I should see all the budgets

  Scenario: A user can retrieve a budget by id
    Given a list of budgets exists
    When I retrieve a budget with id 2000
    Then I should see the budget

  Scenario: When a user retrieves a budget by an id does not exist it should get a 404
    Given a list of budgets exists
    When I retrieve a budget with id 5000
    Then I should get a not found error message

  Scenario: A user fails to retrieve a budget when the id is not valid
    Given a list of budgets exists
    When I retrieve a budget with an invalid id
    Then I should get a validation error message