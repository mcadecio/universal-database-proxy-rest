Feature: Create Wheels

  @cockroach
  Scenario: A user creates a wheel
    When I create a wheel with all the fields
    Then I should get a link to the wheel

  @cockroach
  Scenario: A user tries to create a wheel with the type of an existing wheel
    Given the wheel I am trying to create already exists
    When I create the same wheel
    Then I should be alerted that a wheel with the same type already exists

  Scenario: A user creates a wheel with the optional fields only
    When I create a wheel with the optional fields only
    Then I should be alerted that the wheel is mandatory

  Scenario: A user creates a wheel with the incorrect type for a field
    When I create a wheel with an incorrect value for a field
    Then I should be alerted that the wheel type should be a string