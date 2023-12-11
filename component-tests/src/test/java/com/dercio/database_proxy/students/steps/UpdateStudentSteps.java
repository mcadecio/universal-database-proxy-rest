package com.dercio.database_proxy.students.steps;

import com.dercio.database_proxy.students.Student;
import com.dercio.database_proxy.students.StudentRepository;
import com.dercio.database_proxy.students.StudentService;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateStudentSteps {

    private final StudentContext studentContext;
    private final StudentRepository studentRepository;
    private final StudentService studentService;


    @When("I update a student named {string}")
    public void iUpdateAStudentsNamed(String name) {
        var student = studentContext.getStudents().get(0);
        studentContext.setResponse(studentService.update(name,null, student));
    }

    @When("I update a student using its name and age")
    public void iUpdateAStudentUsingItsNameAndAge() {
        var student = studentContext.getStudents().get(0);
        var updatedStudent = new Student(student.name(), student.age(), "8340959");
        studentContext.setResponse(studentService.update(student.name(), student.age(), updatedStudent));
        studentContext.getStudents().add(0, updatedStudent);
    }

    @Then("I should see the newly updated student")
    public void iShouldSeeTheNewlyUpdatedStudent() {
        var expectedStudent = studentContext.getStudents().get(0);
        studentContext.getResponse().then().statusCode(204);
        var actualStudent = studentRepository.findById(expectedStudent.name(), expectedStudent.age());
        assertEquals(expectedStudent, actualStudent);
    }
}
