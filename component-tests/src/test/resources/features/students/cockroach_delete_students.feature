@cockroach
Feature: Delete Students

  Scenario: A user cannot delete a student only using its name
    Given a list of students
    When I delete a student named "Alex"
    Then I should be alerted that I need to provide the user's age

  Scenario: A user can delete a student using its name and age as the id
    Given a list of students
    When I delete a student using its name and age
    Then the student should be deleted