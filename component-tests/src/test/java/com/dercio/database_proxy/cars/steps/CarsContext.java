package com.dercio.database_proxy.cars.steps;

import com.dercio.database_proxy.cars.Car;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ScenarioScoped
public class CarsContext {
    private final List<Car> cars = new ArrayList<>();
    private Response response;
}
