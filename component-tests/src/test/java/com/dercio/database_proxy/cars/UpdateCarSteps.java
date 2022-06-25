package com.dercio.database_proxy.cars;

import com.dercio.database_proxy.repositories.cars.Car;
import com.dercio.database_proxy.repositories.cars.CarsRepository;
import com.dercio.database_proxy.repositories.cars.CarsService;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static com.dercio.database_proxy.cars.CarFactory.createFiatCar;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ScenarioScoped
public class UpdateCarSteps {

    private final List<Car> cars;
    private final CarsContext carsContext;
    private final CarsRepository carsRepository;
    private final CarsService carsService;

    @Inject
    public UpdateCarSteps(CarsContext carsContext,
                          CarsRepository carsRepository,
                          CarsService carsService) {
        this.cars = carsContext.getCars();
        this.carsContext = carsContext;
        this.carsRepository = carsRepository;
        this.carsService = carsService;
    }

    @Given("a car exists")
    public void aCarExists() {
        var fiatCar = createFiatCar();

        cars.add(fiatCar);

        carsRepository.save(fiatCar);
    }

    @When("I update the id of the car")
    public void iUpdateTheIdOfTheCar() {
        var existingCar = cars.get(0);
        var updatedCar = new Car()
                .setCarId(100000)
                .setDoors(existingCar.getDoors())
                .setManufacturer(existingCar.getManufacturer());

        carsContext.setResponse(carsService.updateCar(existingCar.getCarId(), updatedCar));
    }

    @Then("I should be alerted that the id of the car cannot be updated")
    public void iShouldBeAlertedThatTheIdOfTheCarCannotBeUpdated() {
        carsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/cars/2001"))
                .body("message", containsString("inconsistent primary key values"))
                .body("code", equalTo(400));
    }

    @When("I update the manufacturer to no value")
    public void iUpdateTheManufacturerToNoValue() {
        var existingCar = cars.get(0);
        var updatedCar = new Car()
                .setCarId(existingCar.getCarId())
                .setDoors(existingCar.getDoors())
                .setManufacturer(null);

        carsContext.setResponse(carsService.updateCar(existingCar.getCarId(), updatedCar));
    }

    @Then("I should be alerted that the manufacturer is a required field")
    public void iShouldBeAlertedThatTheManufacturerIsARequiredField() {
        carsContext.getResponse().then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/cars/2001"))
                .body("message", containsString("manufacturer cannot be null"))
                .body("code", equalTo(400));
    }

    @When("I update the manufacturer")
    public void iUpdateTheManufacturer() {
        var existingCar = cars.get(0)
                .setManufacturer("SOMETHING-ELSE");

        carsContext.setResponse(carsService.updateCar(existingCar.getCarId(), existingCar));
    }

    @Then("I should see the manufacturer in the car")
    public void iShouldSeeTheManufacturerInTheCar() {
        var updatedCar = carsRepository.findById(cars.get(0).getCarId());

        assertEquals("SOMETHING-ELSE", updatedCar.getManufacturer());
        assertEquals(cars.get(0), updatedCar);
    }

    @When("I update a car that does not exist")
    public void iUpdateACarThatDoesNotExist() {
        var car = createFiatCar();

        carsContext.setResponse(carsService.updateCar(car.getCarId(), car));
    }
}
