package com.dercio.database_proxy.cars;

import com.dercio.database_proxy.repositories.cars.Car;
import com.dercio.database_proxy.repositories.cars.CarsRepository;
import com.dercio.database_proxy.repositories.cars.CarsService;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;

import static com.dercio.database_proxy.cars.CarFactory.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ScenarioScoped
public class CreateCarSteps {

    private final List<Car> cars;
    private final CarsContext carsContext;
    private final CarsRepository carsRepository;
    private final CarsService carsService;

    @Inject
    public CreateCarSteps(CarsContext carsContext,
                          CarsRepository carsRepository,
                          CarsService carsService) {
        this.cars = carsContext.getCars();
        this.carsContext = carsContext;
        this.carsRepository = carsRepository;
        this.carsService = carsService;
    }

    @When("I create a car with all the fields")
    public void iCreateACarWithAllTheFields() {
        var car = createFiatCar();

        cars.add(car);

        carsContext.setResponse(carsService.createCar(car));
    }

    @Then("I should get a link to the car")
    public void iShouldGetALinkToTheCar() {
        var car = cars.get(0);
        var expectedUrl = "http://localhost:8010/cars/" + car.getCarId();
        carsContext.getResponse().then()
                .statusCode(201)
                .header("Location", expectedUrl);
        assertEquals(car, carsRepository.findById(cars.get(0).getCarId()));
    }

    @When("I create a car with the required fields")
    public void iCreateACarWithTheRequiredFields() {
        var car = createRequiredFieldsCar();

        cars.add(car);

        carsContext.setResponse(carsService.createCar(car));
    }

    @Given("the car I am trying to create already exists")
    @Transactional
    public void theCarIAmTryingToCreateAlreadyExists() {
        var car = createFiatCar();

        cars.add(car);

        carsRepository.save(car);
    }

    @When("I create the same car")
    public void iCreateTheSameCar() {
        carsContext.setResponse(carsService.createCar(cars.get(0)));
    }

    @Then("I should be alerted that a car with the same id already exists")
    public void iShouldBeAlertedThatACarWithTheSameIdAlreadyExists() {
        carsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/cars/"))
                .body("message", equalTo("ERROR: duplicate key value violates unique constraint \"cars_car_id_uindex\" (23505)"))
                .body("code", equalTo(400));
    }

    @When("I create a car with the optional fields only")
    public void iCreateACarWithTheOptionalFieldsOnly() {
        var car = createOptionalFieldsCar();

        cars.add(car);

        carsContext.setResponse(carsService.createCar(car));
    }

    @Then("I should be alerted that the car id is mandatory")
    public void iShouldBeAlertedThatTheCarIdIsMandatory() {
        carsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/cars/"))
                .body("message", equalTo("car_id cannot be null"))
                .body("code", equalTo(400));
    }

    @When("I create a car with an incorrect value for a field")
    public void iCreateACarWithAnIncorrectValueForAField() {
        var car = createFiatCar().setManufacturer(100);

        cars.add(car);

        carsContext.setResponse(carsService.createCar(car));
    }

    @Then("I should be alerted that the manufacturer should be a string")
    public void iShouldBeAlertedThatTheManufacturerShouldBeAString() {
        carsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/cars/"))
                .body("message", equalTo("property 'manufacturer' with value \"100\" is not a valid STRING"))
                .body("code", equalTo(400));
    }
}
