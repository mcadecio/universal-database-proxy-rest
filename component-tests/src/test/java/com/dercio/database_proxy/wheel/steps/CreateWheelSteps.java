package com.dercio.database_proxy.wheel.steps;

import com.dercio.database_proxy.wheel.Wheel;
import com.dercio.database_proxy.wheel.WheelsRepository;
import com.dercio.database_proxy.wheel.WheelsService;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ScenarioScoped
public class CreateWheelSteps {

    private final List<Wheel> wheels;
    private final WheelsContext wheelsContext;
    private final WheelsRepository wheelsRepository;
    private final WheelsService wheelsService;

    @Inject
    public CreateWheelSteps(WheelsContext wheelsContext,
                            WheelsRepository wheelsRepository,
                            WheelsService wheelsService) {
        this.wheels = wheelsContext.getWheels();
        this.wheelsContext = wheelsContext;
        this.wheelsRepository = wheelsRepository;
        this.wheelsService = wheelsService;
    }

    @When("I create a wheel with all the fields")
    public void iCreateAWheelWithAllTheFields() {
        var wheel = new Wheel().setWheelType("STEEL");

        wheels.add(wheel);

        wheelsContext.setResponse(wheelsService.createWheel(wheel));
    }

    @Then("I should get a link to the wheel")
    public void iShouldGetALinkToTheWheel() {
        var wheel = wheels.get(0);
        var expectedUrl = "http://localhost:8010/wheel/" + wheel.getWheelType();
        wheelsContext.getResponse().then()
                .statusCode(201)
                .header("Location", expectedUrl);
        assertEquals(wheel, wheelsRepository.findByType(wheels.get(0).getWheelType().toString()));
    }

    @Given("the wheel I am trying to create already exists")
    @Transactional
    public void theWheelIAmTryingToCreateAlreadyExists() {
        var wheel = new Wheel().setWheelType("FORGED");

        wheels.add(wheel);

        wheelsRepository.save(wheel);
    }

    @When("I create the same wheel")
    public void iCreateTheSameWheel() {
        wheelsContext.setResponse(wheelsService.createWheel(wheels.get(0)));
    }

    @Then("I should be alerted that a wheel with the same type already exists")
    public void iShouldBeAlertedThatAWheelWithTheSameTypeAlreadyExists() {
        wheelsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/wheel/"))
                .body("message", equalTo("ERROR: duplicate key value violates unique constraint \"wheel_pkey\" (23505)"))
                .body("code", equalTo(400));
    }

    @When("I create a wheel with the optional fields only")
    public void iCreateAWheelWithTheOptionalFieldsOnly() {
        var wheel = new Wheel();

        wheels.add(wheel);

        wheelsContext.setResponse(wheelsService.createWheel(wheel));
    }

    @Then("I should be alerted that the wheel is mandatory")
    public void iShouldBeAlertedThatTheWheelIsMandatory() {
        wheelsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/wheel/"))
                .body("message", equalTo("wheel_type cannot be null"))
                .body("code", equalTo(400));
    }

    @When("I create a wheel with an incorrect value for a field")
    public void iCreateAWheelWithAnIncorrectValueForAField() {
        var wheel = new Wheel().setWheelType(5678);

        wheels.add(wheel);

        wheelsContext.setResponse(wheelsService.createWheel(wheel));
    }

    @Then("I should be alerted that the wheel type should be a string")
    public void iShouldBeAlertedThatTheWheelTypeShouldBeAString() {
        wheelsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/wheel/"))
                .body("message", equalTo("property 'wheel_type' with value \"5678\" is not a valid STRING"))
                .body("code", equalTo(400));
    }
}
