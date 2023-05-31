package com.dercio.database_proxy.budgets.steps;

import com.dercio.database_proxy.budgets.Budget;
import io.cucumber.guice.ScenarioScoped;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@ScenarioScoped
public class BudgetsContext {
    private final List<Budget> budgets = new ArrayList<>();
}
