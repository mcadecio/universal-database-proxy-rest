package com.dercio.database_proxy.cars.steps;

import com.dercio.database_proxy.cars.Car;
import com.dercio.database_proxy.cars.CarsRepository;
import com.dercio.database_proxy.cars.CarsService;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static com.dercio.database_proxy.cars.CarFactory.createFiatCar;
import static org.junit.jupiter.api.Assertions.assertNull;

@ScenarioScoped
public class DeleteCarSteps {

    private final List<Car> cars;
    private final CarsContext carsContext;
    private final CarsRepository carsRepository;
    private final CarsService carsService;

    @Inject
    public DeleteCarSteps(CarsContext carsContext,
                          CarsRepository carsRepository,
                          CarsService carsService) {
        this.cars = carsContext.getCars();
        this.carsContext = carsContext;
        this.carsRepository = carsRepository;
        this.carsService = carsService;
    }

    @Given("the car I previously created is no longer valid")
    public void theCarIPreviouslyCreatedIsNoLongerValid() {
        var car = createFiatCar();
        cars.add(car);
        carsRepository.save(car);
    }

    @When("I delete the car")
    public void iDeleteTheCar() {
        carsContext.setResponse(carsService.deleteCarById(cars.get(0).getCarId()));
    }

    @Then("the car should be deleted")
    public void theCarShouldBeDeleted() {
        var response = carsContext.getResponse();
        response.then().statusCode(204);
        assertNull(carsRepository.findById(cars.get(0).getCarId()));
    }

    @When("I delete a car that does not exist")
    public void iDeleteACarThatDoesNotExist() {
        carsContext.setResponse(carsService.deleteCarById(createFiatCar().getCarId()));
    }
}
