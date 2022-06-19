package com.dercio.database_proxy.repositories.budgets;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Budget {
    private Long id;
    private Integer year;
    private Integer month;
    private BigDecimal income;
    private BigDecimal food;
    private BigDecimal rent;
    private BigDecimal savings;
    private BigDecimal discretionary;
    private String userId;
}
