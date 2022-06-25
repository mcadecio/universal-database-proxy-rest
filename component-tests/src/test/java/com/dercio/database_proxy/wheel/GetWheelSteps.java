package com.dercio.database_proxy.wheel;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.repositories.wheel.Wheel;
import com.dercio.database_proxy.repositories.wheel.WheelsRepository;
import com.dercio.database_proxy.repositories.wheel.WheelsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ScenarioScoped
public class GetWheelSteps {

    private final List<Wheel> wheels;
    private final WheelsContext wheelsContext;
    private final WheelsRepository wheelsRepository;
    private final WheelsService wheelsService;
    private final Mapper mapper;

    @Inject
    public GetWheelSteps(WheelsContext wheelsContext,
                         WheelsRepository wheelsRepository,
                         WheelsService wheelsService, Mapper mapper) {
        this.wheels = wheelsContext.getWheels();
        this.wheelsContext = wheelsContext;
        this.wheelsRepository = wheelsRepository;
        this.wheelsService = wheelsService;
        this.mapper = mapper;
    }

    @Given("a list of wheels exists")
    public void aListOfWheelsExists() {
        var steelWheel = new Wheel().setWheelType("STEEL");
        var ceramicWheel = new Wheel().setWheelType("CERAMIC");

        wheels.add(steelWheel);
        wheels.add(ceramicWheel);

        wheelsRepository.save(steelWheel);
        wheelsRepository.save(ceramicWheel);
    }

    @When("I retrieve all the wheels")
    public void iRetrieveAllTheWheels() {
        wheelsContext.setResponse(wheelsService.getWheels());
    }

    @Then("I should see all the wheels")
    public void iShouldSeeAllTheWheels() {
        var response = wheelsContext.getResponse();
        response.then().statusCode(200);
        var wheelsResponse = mapper.decode(
                response.getBody().asString(),
                new TypeReference<List<Wheel>>() {
                }
        );
        assertTrue(wheelsResponse.containsAll(wheels));
    }

    @When("I retrieve a wheel of type {string}")
    public void iRetrieveAWheelOfType(String type) {
        wheelsContext.setResponse(wheelsService.getWheelByType(type));
    }

    @Then("I should see the wheel")
    public void iShouldSeeTheWheel() {
        var response = wheelsContext.getResponse();
        var wheel = mapper.decode(response.body().asString(), new TypeReference<Wheel>() {
        });

        assertTrue(wheels.contains(wheel));
    }

    @When("I retrieve a wheel with an invalid id")
    public void iRetrieveAWheelWithAnInvalidId() {
        wheelsContext.setResponse(wheelsService.getWheelByType(100));
    }

}
