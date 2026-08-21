@cassandra
Feature: Update Albums

  Scenario: A user can update an album
    Given a list of albums exists
    When I update the album "aaaaaaaa-0000-0000-0000-000000000001"
    Then I should see the newly updated album

  Scenario: Updating an album that does not exist does not create it
    When I update an album that does not exist
    Then the album should not be created by the update
