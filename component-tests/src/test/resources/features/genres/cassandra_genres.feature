@cassandra
Feature: Genres where every column is part of the primary key

  Scenario: A user can retrieve a genre by its id
    Given a list of genres exists
    When I retrieve the genre "bebop"
    Then I should see the genre "bebop"

  Scenario: A user can update a genre that has nothing to set
    Given a list of genres exists
    When I update the genre "bebop"
    Then the genre should still exist

  Scenario: Updating a genre that does not exist does not create it
    When I update a genre that does not exist
    Then the genre should not be created by the update

  Scenario: A user can delete every genre
    Given a list of genres exists
    When I delete all the genres
    Then no genres should remain
