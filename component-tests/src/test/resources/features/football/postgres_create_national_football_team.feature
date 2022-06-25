Feature: Create National Football Team

  @postgres
  Scenario: A user creates a national football team
    When I create a national football with all the fields
    Then I should get a link to the national football team

  @postgres
  Scenario: A user creates a national football team with the required fields
    When I create a national football team with the required fields
    Then I should get a link to the national football team

  @postgres
  Scenario: A user tries to create a national football team with the name of an existing national football team
    Given the national football team I am trying to create already exists
    When I create the same national football team
    Then I should be alerted that a national football with the same name already exists

  Scenario: A user creates a national football team with the incorrect type for a field
    When I create a national football team with an incorrect value for a field
    Then I should be alerted that the abbreviated name should be text