Feature: Update Budgets

  @postgres
  Scenario: A user updates a budget and misses a required field
    Given a budget exists
    When I update the month to no value
    Then I should be alerted that the month is a required field

  @postgres
  Scenario: A user updates an optional field in the budget
    Given a budget exists
    When I update the user id field
    Then I should see the new user id in the budget

  @postgres
  Scenario: A user updates an optional field in the budget
    Given a budget exists
    When I update the year field
    Then I should see the new year in the budget

  Scenario: A user updates a budget that does not exist
    When I update a budget that does not exist
    Then I should notified the budget does not exist