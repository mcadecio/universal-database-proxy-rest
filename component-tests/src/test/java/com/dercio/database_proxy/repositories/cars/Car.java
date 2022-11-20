package com.dercio.database_proxy.repositories.cars;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Car {
    private Integer carId;
    private Object manufacturer;
    private Integer doors;
    private OffsetDateTime lastUpdated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return carId.equals(car.carId) &&
                manufacturer.equals(car.manufacturer) &&
                Objects.equals(doors, car.doors) &&
                (lastUpdated != null ? lastUpdated.isEqual(car.lastUpdated) : Objects.equals(lastUpdated, car.lastUpdated));
    }

    @Override
    public int hashCode() {
        return Objects.hash(carId, manufacturer, doors, lastUpdated);
    }
}
