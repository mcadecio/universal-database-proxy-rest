package com.dercio.database_proxy.football;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.repositories.football.NationalFootballTeam;
import com.dercio.database_proxy.repositories.football.NationalFootballTeamService;
import com.dercio.database_proxy.repositories.football.NationalFootballTeamsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;

import static com.dercio.database_proxy.football.FootballFactory.createFranceTeam;
import static com.dercio.database_proxy.football.FootballFactory.createItalyTeam;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@ScenarioScoped
public class GetFootballTeamSteps {

    private final List<NationalFootballTeam> footballTeams;
    private final NationalFootballTeamsRepository footballTeamsRepository;
    private final FootballTeamContext footballTeamContext;
    private final NationalFootballTeamService footballTeamService;
    private final Mapper mapper;

    @Inject
    public GetFootballTeamSteps(FootballTeamContext footballTeamContext,
                                NationalFootballTeamsRepository footballTeamsRepository,
                                NationalFootballTeamService footballTeamService, Mapper mapper) {
        this.footballTeamContext = footballTeamContext;
        this.footballTeamService = footballTeamService;
        this.footballTeamsRepository = footballTeamsRepository;
        this.footballTeams = footballTeamContext.getFootballTeams();
        this.mapper = mapper;
    }

    @Given("a list of football teams exists")
    @Transactional
    public void aListOfFootballTeamsExists() {
        var france = createFranceTeam();
        var italy = createItalyTeam();

        footballTeams.add(france);
        footballTeams.add(italy);

        footballTeamsRepository.save(france);
        footballTeamsRepository.save(italy);
    }

    @When("I retrieve all the football teams")
    public void iRetrieveAllTheFootballTeams() {
        footballTeamContext.setResponse(footballTeamService.getFootballTeams());
    }

    @Then("I should see all the football teams")
    public void iShouldSeeAllTheFootballTeams() {
        var response = footballTeamContext.getResponse();
        response.then().statusCode(200);

        var footballTeamsResponse = mapper.decode(
                response.asString(),
                new TypeReference<List<NationalFootballTeam>>() {
                }
        );

        assertTrue(footballTeamsResponse.containsAll(footballTeams));
    }

    @When("I retrieve a football teams named {string}")
    public void iRetrieveAFootballTeamsNamed(String teamName) {
        footballTeamContext.setResponse(footballTeamService.getFootballTeamByName(teamName));
    }

    @Then("I should see the football team")
    public void iShouldSeeTheFootballTeam() {
        var response = footballTeamContext.getResponse();
        response.then().statusCode(200);

        var footbalTeam = mapper.decode(response.body().asString(), new TypeReference<NationalFootballTeam>() {
        });

        assertTrue(footballTeamContext.getFootballTeams().contains(footbalTeam));
    }

    @When("I retrieve a football teams with an invalid name")
    public void iRetrieveAFootballTeamsWithAnInvalidName() {
        footballTeamContext.setResponse(footballTeamService.getFootballTeamByName(10));
    }

    @Then("I should be alerted that the name is invalid")
    public void iShouldBeAlertedThatTheNameIsInvalid() {
        var response = footballTeamContext.getResponse();
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/budgets/INVALID"))
                .body("message", equalTo("[Bad Request] Parsing error for parameter id in location PATH: java.lang.NumberFormatException: For input string: \"INVALID\""))
                .body("code", equalTo(400));
    }
}
