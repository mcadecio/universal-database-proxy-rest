@postgres
Feature: Get National Football Team

  Scenario: A user can retrieve a list of football teams
    Given a list of football teams exists
    When I retrieve all the football teams
    Then I should see all the football teams

  Scenario: A user can retrieve a football teams by name
    Given a list of football teams exists
    When I retrieve a football teams named "FRANCE"
    Then I should see the football team

  Scenario: When a user retrieves a football teams by a name that does not exist they should get a 404
    Given a list of football teams exists
    When I retrieve a football teams named "AMERICA"
    Then I should be alerted that the national football team does not exist

  Scenario: A user fails to retrieve a football teams when the id is not valid
    Given a list of football teams exists
    When I retrieve a football teams with an invalid name
    Then I should be alerted that the national football team does not exist