package com.dercio.database_proxy.students.steps;

import com.dercio.database_proxy.students.StudentFactory;
import com.dercio.database_proxy.students.StudentRepository;
import com.dercio.database_proxy.students.StudentService;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CreateStudentSteps {
    private final StudentContext studentContext;
    private final StudentRepository studentRepository;
    private final StudentService studentService;

    @When("I create a student with all the fields")
    public void iCreateAStudentWithAllTheFields() {
        var student = StudentFactory.createAlexStudent();

        studentContext.getStudents().add(student);

        studentContext.setResponse(studentService.create(student));
    }

    @Then("I should get a link to the student")
    public void iShouldGetALinkToTheStudent() {
        var student = studentContext.getStudents().get(0);
        var expectedUrl = "http://localhost:8010/students/" + student.name() + ":" + student.age();
        studentContext.getResponse()
                .then()
                .statusCode(201)
                .header("Location", expectedUrl);
        assertEquals(student, studentRepository.findById(student.name(), student.age()));
    }
}
