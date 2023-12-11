@cockroach
Feature: Create a Student

  Scenario: A user can create a student
    When I create a student with all the fields
    Then I should get a link to the student