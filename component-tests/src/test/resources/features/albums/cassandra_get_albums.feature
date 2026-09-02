@cassandra
Feature: Get Albums

  Scenario: A user can retrieve a list of albums
    Given a list of albums exists
    When I retrieve all the albums
    Then I should see the albums I created

  Scenario: A user can filter albums by a column that is not part of the primary key
    Given a list of albums exists
    When I retrieve all the albums by artist "Coltrane Quartet"
    Then I should see only albums by "Coltrane Quartet"

  Scenario: A user can retrieve an album by its id
    Given a list of albums exists
    When I retrieve the album "aaaaaaaa-0000-0000-0000-000000000001"
    Then I should see the album titled "Blue Train"
    And I should see the album's tags and ratings

  Scenario: A user can filter albums by membership of a set column
    Given a list of albums exists
    When I retrieve all the albums tagged "hard-bop"
    Then I should see only albums tagged "hard-bop"

  Scenario: A user can filter albums by membership of a set of integers
    Given a list of albums exists
    When I retrieve all the albums rated 5
    Then I should see the album rated 5 but not the others

  Scenario: A user retrieving an album that does not exist
    Given a list of albums exists
    When I retrieve the album "aaaaaaaa-ffff-ffff-ffff-ffffffffffff"
    Then I should get an album not found error
