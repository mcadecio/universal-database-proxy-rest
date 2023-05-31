package com.dercio.database_proxy.wheel.steps;

import com.dercio.database_proxy.wheel.Wheel;
import com.dercio.database_proxy.wheel.WheelsRepository;
import com.dercio.database_proxy.wheel.WheelsService;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.hamcrest.Matchers.*;

@ScenarioScoped
public class UpdateWheelSteps {

    private final List<Wheel> wheels;
    private final WheelsContext wheelsContext;
    private final WheelsRepository wheelsRepository;
    private final WheelsService wheelsService;

    @Inject
    public UpdateWheelSteps(WheelsContext wheelsContext,
                            WheelsRepository wheelsRepository,
                            WheelsService wheelsService) {
        this.wheels = wheelsContext.getWheels();
        this.wheelsContext = wheelsContext;
        this.wheelsRepository = wheelsRepository;
        this.wheelsService = wheelsService;
    }

    @Given("a wheel exists")
    public void aWheelExists() {
        var wheel = new Wheel().setWheelType("COPPER");

        wheels.add(wheel);

        wheelsRepository.save(wheel);
    }

    @When("I update the type of the wheel")
    public void iUpdateTheTypeOfTheWheel() {
        var existingWheel = wheels.get(0);
        var updatedWheel = new Wheel().setWheelType("OTHER");

        wheelsContext.setResponse(wheelsService.updateWheel(existingWheel.getWheelType().toString(), updatedWheel));
    }

    @Then("I should be alerted that tables with only one column cannot be updated")
    public void iShouldBeAlertedThatTablesWithOnlyOneColumnCannotBeUpdated() {
        wheelsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/wheel/" + wheels.get(0).getWheelType()))
                .body("message", containsString("Unable to update table with only one column"))
                .body("code", equalTo(400));
    }

    @When("I update the wheel with no value")
    public void iUpdateTheWheelWithNoValue() {
        var existingWheel = wheels.get(0);

        wheelsContext.setResponse(wheelsService.updateWheel(existingWheel.getWheelType().toString(), null));
    }

    @Then("I should be alerted that the wheel is a required field")
    public void iShouldBeAlertedThatTheWheelIsARequiredField() {
        wheelsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/wheel/COPPER"))
                .body("message", containsString("body cannot be null"))
                .body("code", equalTo(400));
    }

    @When("I update a wheel that does not exist")
    public void iUpdateAWheelThatDoesNotExist() {
        var wheel = new Wheel().setWheelType("ALLOY");

        wheels.add(wheel);

        wheelsContext.setResponse(wheelsService.updateWheel(wheel.getWheelType().toString(), wheel));
    }
}
