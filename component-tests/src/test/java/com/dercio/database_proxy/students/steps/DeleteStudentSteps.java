package com.dercio.database_proxy.students.steps;

import com.dercio.database_proxy.students.StudentRepository;
import com.dercio.database_proxy.students.StudentService;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DeleteStudentSteps {
    private final StudentContext studentContext;
    private final StudentRepository studentRepository;
    private final StudentService studentService;

    @When("I delete a student named {string}")
    public void iDeleteAStudentsNamed(String name) {
        studentContext.setResponse(studentService.deleteById(name, null));
    }

    @When("I delete a student using its name and age")
    public void iDeleteAStudentNamedAndAge() {
        var student = studentContext.getStudents().get(0);
        studentContext.setResponse(studentService.deleteById(student.name(), student.age()));
    }

    @Then("the student should be deleted")
    public void theStudentShouldBeDeleted() {
        var student = studentContext.getStudents().get(0);
        var response = studentContext.getResponse();
        response.then().statusCode(204);
        studentRepository.findById(student.name(), student.age());
    }
}
