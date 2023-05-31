package com.dercio.database_proxy.football.steps;

import com.dercio.database_proxy.football.NationalFootballTeam;
import com.dercio.database_proxy.football.NationalFootballTeamService;
import com.dercio.database_proxy.football.NationalFootballTeamsRepository;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;

import static com.dercio.database_proxy.football.FootballFactory.createFranceTeam;
import static com.dercio.database_proxy.football.FootballFactory.createInvalidFootbalTeam;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ScenarioScoped
public class CreateFootballTeamSteps {

    private final List<NationalFootballTeam> footballTeams;
    private final NationalFootballTeamsRepository footballTeamsRepository;
    private final NationalFootballTeamService footballTeamService;

    private Response response;

    @Inject
    public CreateFootballTeamSteps(FootballTeamContext footballTeamContext,
                                   NationalFootballTeamsRepository footballTeamsRepository,
                                   NationalFootballTeamService footballTeamService) {
        this.footballTeamService = footballTeamService;
        this.footballTeamsRepository = footballTeamsRepository;
        this.footballTeams = footballTeamContext.getFootballTeams();
    }


    @When("I create a national football with all the fields")
    public void iCreateANationalFootballWithAllTheFields() {
        var footbalTeam = createFranceTeam();
        footballTeams.add(footbalTeam);
        response = footballTeamService.createFootballTeam(footbalTeam);
    }

    @Then("I should get a link to the national football team")
    public void iShouldGetALinkToTheNationalFootballTeam() {
        var nationalFootballTeam = footballTeams.get(0);
        var expectedUrl = "http://localhost:8000/national_football_teams/" + nationalFootballTeam.getName();
        response.then()
                .statusCode(201)
                .header("Location", expectedUrl);
        assertEquals(nationalFootballTeam, footballTeamsRepository.findTeamByName(footballTeams.get(0).getName()));
    }

    @When("I create a national football team with the required fields")
    public void iCreateANationalFootballTeamWithTheRequiredFields() {
        iCreateANationalFootballWithAllTheFields();
    }


    @Given("the national football team I am trying to create already exists")
    @Transactional
    public void theNationalFootballTeamIAmTryingToCreateAlreadyExists() {
        var footbalTeam = createFranceTeam();
        footballTeams.add(footbalTeam);
        footballTeamsRepository.save(footbalTeam);
    }

    @When("I create the same national football team")
    public void iCreateTheSameNationalFootballTeam() {
        response = footballTeamService.createFootballTeam(footballTeams.get(0));
    }

    @Then("I should be alerted that a national football with the same name already exists")
    public void iShouldBeAlertedThatANationalFootballWithTheSameNameAlreadyExists() {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/national_football_teams/"))
                .body("message", equalTo("ERROR: duplicate key value violates unique constraint \"national_football_teams_pkey\" (23505)"))
                .body("code", equalTo(400));
    }

    @When("I create a national football team with an incorrect value for a field")
    public void iCreateANationalFootballTeamWithAnIncorrectValueForAField() {
        var invalidFootballTeam = createInvalidFootbalTeam();
        response = footballTeamService.createFootballTeam(invalidFootballTeam);
    }

    @Then("I should be alerted that the abbreviated name should be text")
    public void iShouldBeAlertedThatTheAbbreviatedNameShouldBeText() {
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/national_football_teams/"))
                .body("message", equalTo("property 'abbreviated_name' with value \"2021\" is not a valid STRING"))
                .body("code", equalTo(400));
    }

}
