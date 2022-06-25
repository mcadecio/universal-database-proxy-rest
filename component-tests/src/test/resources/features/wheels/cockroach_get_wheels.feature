@cockroach
Feature: Get Wheels

  Scenario: A user can retrieve a list of wheels
    Given a list of wheels exists
    When I retrieve all the wheels
    Then I should see all the wheels

  Scenario: A user can retrieve a wheel by type
    Given a list of wheels exists
    When I retrieve a wheel of type "STEEL"
    Then I should see the wheel

  Scenario: When a user retrieves a wheel by a type that does not exist they should get a 404
    Given a list of wheels exists
    When I retrieve a wheel of type "UNKOWN"
    Then I should be alerted that the wheel does not exist

  Scenario: A user fails to retrieve a wheel when the type is not valid
    Given a list of wheels exists
    When I retrieve a wheel with an invalid id
    Then I should be alerted that the wheel does not exist