Feature: Update a Wheel

  @cockroach
  Scenario: A user updates a wheel
    Given a wheel exists
    When I update the type of the wheel
    Then I should be alerted that tables with only one column cannot be updated

  @cockroach
  Scenario: A user updates a wheel and misses a required field
    Given a wheel exists
    When I update the wheel with no value
    Then I should be alerted that the wheel is a required field

  Scenario: A user updates a wheel that does not exist
    When I update a wheel that does not exist
    Then I should be alerted that tables with only one column cannot be updated