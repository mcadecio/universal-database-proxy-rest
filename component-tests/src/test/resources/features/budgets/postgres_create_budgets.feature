Feature: Create Budgets

  @postgres
  Scenario: A user creates a budget
    When I create a budget with all the fields
    Then I should get a link to the budget

  @postgres
  Scenario: A user creates a budget with the required fields only
    When I create a budget with the required fields only
    Then I should get a link to the budget

  @postgres
  Scenario: A user tries to create a budget with the id of an existing budget
    Given the budget I am trying to create already exists
    When I create the same budget
    Then I should get an error message

  Scenario: A user creates a budget with the optional fields only
    When I create a budget with the optional fields only
    Then I should get a the following validation error message: "id cannot be null"

  Scenario: A user creates a budget with the incorrect type for a field
    When I create a budget with an incorrect value for a field
    Then I should get a the following validation error message: "property 'month' with value \"November\" is not a valid INTEGER"