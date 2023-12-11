package com.dercio.database_proxy.students.steps;

import com.dercio.database_proxy.students.Student;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ScenarioScoped
public class StudentContext {

    private final List<Student> students = new ArrayList<>();
    private Response response;
}
