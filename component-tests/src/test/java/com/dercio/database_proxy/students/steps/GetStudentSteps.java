package com.dercio.database_proxy.students.steps;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.students.Student;
import com.dercio.database_proxy.students.StudentFactory;
import com.dercio.database_proxy.students.StudentRepository;
import com.dercio.database_proxy.students.StudentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GetStudentSteps {

    private final Mapper mapper;
    private final StudentContext studentContext;
    private final StudentRepository studentRepository;
    private final StudentService studentService;

    @Given("a list of students")
    public void aListOfStudents() {
        var alexStudent = StudentFactory.createAlexStudent();
        var davidStudent = StudentFactory.createDavidStudent();
        var manuelStudent = StudentFactory.createManuelStudent();

        studentContext.getStudents().add(alexStudent);
        studentContext.getStudents().add(davidStudent);
        studentContext.getStudents().add(manuelStudent);

        studentRepository.save(alexStudent);
        studentRepository.save(davidStudent);
        studentRepository.save(manuelStudent);
    }

    @When("I retrieve all the students named {string}")
    public void iRetrieveAllTheStudentsNamed(String name) {
        studentContext.setResponse(studentService.getAll(Map.of("name", name)));
    }

    @Then("I should see only students named {string}")
    public void iShouldSeeOnlyStudentsNamed(String name) {
        var studentsResponse = mapper.decode(studentContext.getResponse().body().asString(), new TypeReference<List<Student>>() {});
        assertEquals(1, studentsResponse.size());

        var student = studentsResponse.get(0);
        assertEquals(name, student.name());
        assertEquals(10, student.age());
    }

    @When("I retrieve a student named {string}")
    public void iRetrieveAStudentNamed(String name) {
        studentContext.setResponse(studentService.getById(name, null));
    }

    @Then("I should be alerted that I need to provide the user's age")
    public void iShouldBeAlertedThatINeedToProvideTheUserSAge() {
        studentContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", matchesRegex("/students/Alex:null"))
                .body("message", equalTo("[Bad Request] Parsing error for parameter age in location PATH: java.lang.NumberFormatException: For input string: \"null\""))
                .body("code", equalTo(400));
    }

    @When("I retrieve a student named {string} and age {int}")
    public void iRetrieveAStudentNamedAndAge(String name, int age) {
        studentContext.setResponse(studentService.getById(name, age));

    }

    @Then("I should see the student")
    public void iShouldSeeTheStudent() {
        var student = mapper.decode(studentContext.getResponse().body().asString(), Student.class);
        assertEquals("Alex", student.name());
        assertEquals(10, student.age());
        assertEquals("312312", student.phone());
    }
}
