package com.dercio.database_proxy.wheel;

import com.dercio.database_proxy.repositories.wheel.Wheel;
import com.dercio.database_proxy.repositories.wheel.WheelsRepository;
import com.dercio.database_proxy.repositories.wheel.WheelsService;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

@ScenarioScoped
public class DeleteWheelSteps {

    private final List<Wheel> wheels;
    private final WheelsContext wheelsContext;
    private final WheelsRepository wheelsRepository;
    private final WheelsService wheelsService;

    @Inject
    public DeleteWheelSteps(WheelsContext wheelsContext,
                            WheelsRepository wheelsRepository,
                            WheelsService wheelsService) {
        this.wheels = wheelsContext.getWheels();
        this.wheelsContext = wheelsContext;
        this.wheelsRepository = wheelsRepository;
        this.wheelsService = wheelsService;
    }

    @Given("the wheel I previously created is no longer valid")
    public void theWheelIPreviouslyCreatedIsNoLongerValid() {
        var wheel = new Wheel().setWheelType("ALLOY");
        wheels.add(wheel);
        wheelsRepository.save(wheel);
    }

    @When("I delete the wheel")
    public void iDeleteTheWheel() {
        wheelsContext.setResponse(wheelsService.deleteWheelByType(wheels.get(0).getWheelType().toString()));
    }

    @Then("the wheel should be deleted")
    public void theWheelShouldBeDeleted() {
        var response = wheelsContext.getResponse();
        response.then().statusCode(204);
        assertNull(wheelsRepository.findByType(wheels.get(0).getWheelType().toString()));
    }

    @When("I delete a wheel that does not exist")
    public void iDeleteAWheelThatDoesNotExist() {
        wheelsContext.setResponse(wheelsService.deleteWheelByType("PLASTIC"));
    }
}
