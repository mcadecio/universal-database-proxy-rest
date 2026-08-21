@cassandra
Feature: Delete Albums

  Scenario: A user can delete an album by its id
    Given a list of albums exists
    When I delete the album "aaaaaaaa-0000-0000-0000-000000000001"
    Then the album should be deleted

  Scenario: Deleting an album that does not exist
    When I delete the album "aaaaaaaa-ffff-ffff-ffff-ffffffffffff"
    Then I should get an album not found error for the delete

  Scenario: A user can delete albums matching a filter
    Given a list of albums exists
    When I delete all the albums by artist "Coltrane Quartet"
    Then only the albums by "Sonny Rollins" should remain
