package com.dercio.database_proxy.cars.steps;

import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

@ScenarioScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CarSteps {

    private final CarsContext context;

    @Then("I should be alerted that the car does not exist")
    public void iShouldBeAlertedThatTheCarDoesNotExist() {
        context.getResponse().then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("path", matchesRegex("/cars/.*"))
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }
}
