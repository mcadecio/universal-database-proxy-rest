package com.dercio.database_proxy.budgets;

public interface BudgetsRepository {

    Budget findById(long id);

    void save(Budget budget);

    void deleteById(long id);
}
