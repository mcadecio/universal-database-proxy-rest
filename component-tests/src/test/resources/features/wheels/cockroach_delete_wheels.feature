Feature: Delete Wheel

  @cockroach
  Scenario: A user deletes a wheel
    Given the wheel I previously created is no longer valid
    When I delete the wheel
    Then the wheel should be deleted

  Scenario: A user deletes a wheel that does not exist
    When I delete a wheel that does not exist
    Then I should be alerted that the wheel does not exist