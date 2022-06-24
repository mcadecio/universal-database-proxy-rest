Feature: Delete Budgets

  @postgres
  Scenario: A user deletes a budget
    Given the budget I previously created is no longer valid
    When I delete the budget
    Then the budget should be deleted

  Scenario: A user deletes a budget that does not exist
    When I delete a budget that does not exist
    Then I should be alerted that the budget does not exist