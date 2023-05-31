package com.dercio.database_proxy.cars.steps;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.cars.Car;
import com.dercio.database_proxy.cars.CarsRepository;
import com.dercio.database_proxy.cars.CarsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static com.dercio.database_proxy.cars.CarFactory.createFerrariCar;
import static com.dercio.database_proxy.cars.CarFactory.createFiatCar;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ScenarioScoped
public class GetCarSteps {

    private final List<Car> cars;
    private final CarsContext carsContext;
    private final CarsRepository carsRepository;
    private final CarsService carsService;
    private final Mapper mapper;

    @Inject
    public GetCarSteps(CarsContext carsContext,
                       CarsRepository carsRepository,
                       CarsService carsService, Mapper mapper) {
        this.cars = carsContext.getCars();
        this.carsContext = carsContext;
        this.carsRepository = carsRepository;
        this.carsService = carsService;
        this.mapper = mapper;
    }

    @Given("a list of cars exists")
    public void aListOfCarsExists() {
        var fiatCar = createFiatCar();
        var ferrariCar = createFerrariCar();

        cars.add(fiatCar);
        cars.add(ferrariCar);

        carsRepository.save(fiatCar);
        carsRepository.save(ferrariCar);
    }

    @When("I retrieve all the cars")
    public void iRetrieveAllTheCars() {
        carsContext.setResponse(carsService.getCars());
    }

    @Then("I should see all the cars")
    public void iShouldSeeAllTheCars() {
        var response = carsContext.getResponse();
        response.then().statusCode(200);
        var carsResponse = mapper.decode(
                response.getBody().asString(),
                new TypeReference<List<Car>>() {
                }
        );
        assertTrue(carsResponse.containsAll(cars));
    }

    @When("I retrieve a cars with id {int}")
    public void iRetrieveACarsWithId(int id) {
        carsContext.setResponse(carsService.getCarById(id));
    }

    @Then("I should see the car")
    public void iShouldSeeTheCar() {
        var response = carsContext.getResponse();
        var car = mapper.decode(response.body().asString(), new TypeReference<Car>() {
        });

        assertTrue(cars.contains(car));
    }

    @When("I retrieve a cars with an invalid id")
    public void iRetrieveACarsWithAnInvalidId() {
        carsContext.setResponse(carsService.getCarById("INVALID"));
    }

    @Then("I should be alerted that the id must be an integer")
    public void iShouldBeAlertedThatTheIdMustBeAnInteger() {
        carsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", matchesRegex("/cars/INVALID"))
                .body("message", equalTo("[Bad Request] Parsing error for parameter car_id in location PATH: java.lang.NumberFormatException: For input string: \"INVALID\""))
                .body("code", equalTo(400));
    }
}
