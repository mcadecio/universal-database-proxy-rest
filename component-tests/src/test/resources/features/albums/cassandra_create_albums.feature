@cassandra
Feature: Create Albums

  Scenario: A user can create an album
    When I create a new album
    Then the album should be created

  Scenario: Creating an album returns a link to it
    When I create a new album
    Then the response should point at the new album
