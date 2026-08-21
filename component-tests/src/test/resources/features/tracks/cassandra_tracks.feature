@cassandra
Feature: Tracks with a composite primary key

  Scenario: A user can filter tracks by the partition key
    Given a list of tracks exists
    When I retrieve all the tracks of the album
    Then I should see both tracks of that album

  Scenario: A user can retrieve a track using the album id and track number as the id
    Given a list of tracks exists
    When I retrieve the track number 1 of the album
    Then I should see the track titled "Moment's Notice"

  Scenario: A user can update a track using the album id and track number as the id
    Given a list of tracks exists
    When I update the track number 1 of the album
    Then I should see the newly updated track

  Scenario: A user can delete a track using the album id and track number as the id
    Given a list of tracks exists
    When I delete the track number 2 of the album
    Then the track should be deleted

  Scenario: Deleting a track that does not exist
    When I delete the track number 99 of the album
    Then I should get a track not found error
