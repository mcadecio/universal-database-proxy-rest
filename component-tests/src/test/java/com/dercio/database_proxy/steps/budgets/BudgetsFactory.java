package com.dercio.database_proxy.steps.budgets;

import com.dercio.database_proxy.repositories.budgets.Budget;

import java.math.BigDecimal;
import java.util.UUID;

public class BudgetsFactory {
    static Budget createJanuaryBudget() {
        return new Budget(
                2_000L,
                2022,
                1,
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(10),
                UUID.randomUUID().toString().substring(0, 15)
        );
    }

    static Budget createFebBudget() {
        return new Budget(
                3_000L,
                2022,
                2,
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(700),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(0),
                UUID.randomUUID().toString().substring(0, 15)
        );
    }

    static Budget createOptionalBudget() {
        return new Budget(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                UUID.randomUUID().toString().substring(0, 15)
        );
    }

    static Budget createRequiredBudget() {
        return new Budget(
                3_000L,
                2022,
                2,
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(700),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(0),
                UUID.randomUUID().toString().substring(0, 15)
        );
    }

    static Budget createBudgetWithInvalidFieldType() {
        return new Budget(
                3_000L,
                2022,
                "November",
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(700),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(0),
                UUID.randomUUID().toString().substring(0, 15)
        );
    }
}
