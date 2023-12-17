package com.dercio.database_proxy.budgets;

import java.util.List;

public interface BudgetsRepository {

    List<Budget> find();

    Budget findById(long id);

    void save(Budget budget);

    void deleteById(long id);
}
