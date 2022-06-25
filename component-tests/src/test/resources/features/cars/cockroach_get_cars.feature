@cockroach
Feature: Get Cars

  Scenario: A user can retrieve a list of cars
    Given a list of cars exists
    When I retrieve all the cars
    Then I should see all the cars

  Scenario: A user can retrieve a cars by id
    Given a list of cars exists
    When I retrieve a cars with id 2001
    Then I should see the car

  Scenario: When a user retrieves a cars by a id that does not exist they should get a 404
    Given a list of cars exists
    When I retrieve a cars with id 999
    Then I should be alerted that the car does not exist

  Scenario: A user fails to retrieve a cars when the id is not valid
    Given a list of cars exists
    When I retrieve a cars with an invalid id
    Then I should be alerted that the id must be an integer