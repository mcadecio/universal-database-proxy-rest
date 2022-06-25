package com.dercio.database_proxy.wheel;

import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

import static org.hamcrest.Matchers.*;

@ScenarioScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WheelSteps {

    private final WheelsContext context;

    @Then("I should be alerted that the wheel does not exist")
    public void iShouldBeAlertedThatTheWheelDoesNotExist() {
        context.getResponse().then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("path", matchesRegex("/wheel/.*"))
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }
}
