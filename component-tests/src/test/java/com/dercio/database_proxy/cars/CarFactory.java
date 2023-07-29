package com.dercio.database_proxy.cars;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class CarFactory {

    public static Car createFiatCar() {
        return new Car()
                .setCarId(2001)
                .setDoors(5)
                .setManufacturer("FIAT")
                .setLastUpdated(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .setExtraDetails(new ExtraDetails().setColor("white"));
    }

    public static Car createFerrariCar() {
        return new Car()
                .setCarId(1997)
                .setDoors(3)
                .setManufacturer("FERRARI")
                .setLastUpdated(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .setExtraDetails(new ExtraDetails().setColor("red"));
    }

    public static Car createRequiredFieldsCar() {
        return new Car()
                .setCarId(678)
                .setManufacturer("PAGANI");
    }

    public static Car createOptionalFieldsCar() {
        return new Car()
                .setDoors(3)
                .setLastUpdated(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .setExtraDetails(new ExtraDetails().setRating("bad"));
    }

}
