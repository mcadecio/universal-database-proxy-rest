Feature: Delete National Football Team

  @postgres
  Scenario: A user deletes a national football team
    Given the national football team I previously created is no longer valid
    When I delete the national football team
    Then the national football team should be deleted

  Scenario: A user deletes a national football team that does not exist
    When I delete a national football team that does not exist
    Then I should be alerted that the national football team does not exist