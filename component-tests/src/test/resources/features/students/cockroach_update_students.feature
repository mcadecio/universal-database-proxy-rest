@cockroach
Feature: Update Students

  Scenario: A user can update a student using its name and age as the id
    Given a list of students
    When I update a student using its name and age
    Then I should see the newly updated student

  Scenario: When a user updates a student without specifying the age
    Given a list of students
    When I update a student named "Alex"
    Then I should be alerted that I need to provide the user's age