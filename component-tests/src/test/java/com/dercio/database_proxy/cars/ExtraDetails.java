package com.dercio.database_proxy.cars;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ExtraDetails {
    private String color;
    private String rating;
}