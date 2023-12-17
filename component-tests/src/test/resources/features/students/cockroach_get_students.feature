@cockroach
Feature: Get Students

  Scenario: A user can retrieve a list of students
    Given a list of students
    When I retrieve all the students named "Alex"
    Then I should see only students named "Alex"

  Scenario: A user can retrieve a student using its name and age as the id
    Given a list of students
    When I retrieve a student named "Alex" and age 10
    Then I should see the student

  Scenario: When a user retrieves a student without specifying the age
    Given a list of students
    When I retrieve a student named "Alex"
    Then I should be alerted that I need to provide the user's age