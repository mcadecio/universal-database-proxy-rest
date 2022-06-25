Feature: Delete Car

  @cockroach
  Scenario: A user deletes a car
    Given the car I previously created is no longer valid
    When I delete the car
    Then the car should be deleted

  Scenario: A user deletes a car that does not exist
    When I delete a car that does not exist
    Then I should be alerted that the car does not exist