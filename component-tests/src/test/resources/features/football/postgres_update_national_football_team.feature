Feature: Update a National Football Team

  @postgres
  Scenario: A user updates a national football team and misses a required field
    Given a national football team exists
    When I update the abbreviated name to no value
    Then I should be alerted that the abbreviated name is a required field

  @postgres
  Scenario: A user updates the abbreviated named of a football team
    Given a national football team exists
    When I update the abbreviated name
    Then I should see the abbreviated name in the national football team

  Scenario: A user updates a national football team that does not exist
    When I update a national football team that does not exist
    Then I should be alerted that the national football team does not exist