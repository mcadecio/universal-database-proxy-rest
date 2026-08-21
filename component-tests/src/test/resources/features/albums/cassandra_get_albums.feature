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

  Scenario: A user retrieving an album that does not exist
    Given a list of albums exists
    When I retrieve the album "aaaaaaaa-ffff-ffff-ffff-ffffffffffff"
    Then I should get an album not found error
