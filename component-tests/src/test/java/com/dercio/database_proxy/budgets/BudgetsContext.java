package com.dercio.database_proxy.budgets;

import com.dercio.database_proxy.repositories.budgets.Budget;
import io.cucumber.guice.ScenarioScoped;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@ScenarioScoped
public class BudgetsContext {
    private final List<Budget> budgets = new ArrayList<>();
}
