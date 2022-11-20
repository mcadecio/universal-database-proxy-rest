package com.dercio.database_proxy.repositories.budgets;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Budget {
    private Long id;
    private Integer year;
    private Object month;
    private BigDecimal income;
    private BigDecimal food;
    private BigDecimal rent;
    private BigDecimal savings;
    private BigDecimal discretionary;
    private String userId;
    private LocalDateTime created;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return id.equals(budget.id) &&
                year.equals(budget.year) &&
                month.equals(budget.month) &&
                income.equals(budget.income) &&
                food.equals(budget.food) &&
                rent.equals(budget.rent) &&
                savings.equals(budget.savings) &&
                discretionary.equals(budget.discretionary) &&
                Objects.equals(userId, budget.userId) &&
                (created != null ? created.isEqual(budget.getCreated()) : Objects.equals(created, budget.getCreated()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, year, month, income, food, rent, savings, discretionary, userId, created);
    }
}
