package com.dercio.database_proxy.cars;

import com.dercio.database_proxy.repositories.cars.Car;
import com.dercio.database_proxy.repositories.cars.ExtraDetails;

import java.time.OffsetDateTime;

public class CarFactory {

    static Car createFiatCar() {
        return new Car()
                .setCarId(2001)
                .setDoors(5)
                .setManufacturer("FIAT")
                .setLastUpdated(OffsetDateTime.now())
                .setExtraDetails(new ExtraDetails().setColor("white"));
    }

    static Car createFerrariCar() {
        return new Car()
                .setCarId(1997)
                .setDoors(3)
                .setManufacturer("FERRARI")
                .setLastUpdated(OffsetDateTime.now())
                .setExtraDetails(new ExtraDetails().setColor("red"));
    }

    static Car createRequiredFieldsCar() {
        return new Car()
                .setCarId(678)
                .setManufacturer("PAGANI");
    }

    static Car createOptionalFieldsCar() {
        return new Car()
                .setDoors(3)
                .setLastUpdated(OffsetDateTime.now())
                .setExtraDetails(new ExtraDetails().setRating("bad"));
    }

}
