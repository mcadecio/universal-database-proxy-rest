package com.dercio.database_proxy.football.steps;

import com.dercio.database_proxy.football.NationalFootballTeam;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@ScenarioScoped
@Setter
public class FootballTeamContext {
    private final List<NationalFootballTeam> footballTeams = new ArrayList<>();
    private Response response;
}
