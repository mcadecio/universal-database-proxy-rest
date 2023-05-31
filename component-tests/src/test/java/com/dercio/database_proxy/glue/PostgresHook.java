package com.dercio.database_proxy.glue;

import com.dercio.database_proxy.budgets.steps.BudgetsContext;
import com.dercio.database_proxy.football.steps.FootballTeamContext;
import com.dercio.database_proxy.budgets.BudgetsRepository;
import com.dercio.database_proxy.football.NationalFootballTeamsRepository;
import com.google.inject.Inject;
import io.cucumber.java.After;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.guice.transactional.Transactional;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PostgresHook {

    private final BudgetsContext budgetsContext;
    private final BudgetsRepository budgetsRepository;

    private final FootballTeamContext footballTeamContext;
    private final NationalFootballTeamsRepository footballTeamsRepository;

    @Transactional
    @After("@postgres")
    public void afterScenario() {
        log.info("Cleaning up scenario");

        log.info("Deleting {} budgets from scenario", budgetsContext.getBudgets().size());
        budgetsContext.getBudgets().forEach(budget -> budgetsRepository.deleteById(budget.getId()));

        var footballTeams = footballTeamContext.getFootballTeams();
        log.info("Deleting {} national football teams from scenario", footballTeams.size());
        footballTeams.forEach(footballTeam -> footballTeamsRepository.deleteByName(footballTeam.getName()));
    }
}
