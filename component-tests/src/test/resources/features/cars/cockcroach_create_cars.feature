Feature: Create Car

  @cockroach
  Scenario: A user creates a car
    When I create a car with all the fields
    Then I should get a link to the car

  @cockroach
  Scenario: A user creates a car with the required fields
    When I create a car with the required fields
    Then I should get a link to the car

  @cockroach
  Scenario: A user tries to create a car with the id of an existing car
    Given the car I am trying to create already exists
    When I create the same car
    Then I should be alerted that a car with the same id already exists

  Scenario: A user creates a car with the optional fields only
    When I create a car with the optional fields only
    Then I should be alerted that the car id is mandatory

  Scenario: A user creates a car with the incorrect type for a field
    When I create a car with an incorrect value for a field
    Then I should be alerted that the manufacturer should be a string