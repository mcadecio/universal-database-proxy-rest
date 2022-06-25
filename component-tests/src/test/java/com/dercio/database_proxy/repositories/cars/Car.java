package com.dercio.database_proxy.repositories.cars;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Car {
    private Integer carId;
    private Object manufacturer;
    private Integer doors;
}
