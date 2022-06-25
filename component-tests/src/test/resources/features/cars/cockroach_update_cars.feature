Feature: Update a Car

  @cockroach
  Scenario: A user updates a car
    Given a car exists
    When I update the id of the car
    Then I should be alerted that the id of the car cannot be updated

  @cockroach
  Scenario: A user updates a car and misses a required field
    Given a car exists
    When I update the manufacturer to no value
    Then I should be alerted that the manufacturer is a required field

  @cockroach
  Scenario: A user updates the manufacturer of a car
    Given a car exists
    When I update the manufacturer
    Then I should see the manufacturer in the car

  Scenario: A user updates a car that does not exist
    When I update a car that does not exist
    Then I should be alerted that the car does not exist