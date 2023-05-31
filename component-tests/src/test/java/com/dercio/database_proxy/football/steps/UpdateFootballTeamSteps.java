package com.dercio.database_proxy.football.steps;

import com.dercio.database_proxy.football.NationalFootballTeam;
import com.dercio.database_proxy.football.NationalFootballTeamService;
import com.dercio.database_proxy.football.NationalFootballTeamsRepository;
import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mybatis.guice.transactional.Transactional;

import java.util.List;
import java.util.Map;

import static com.dercio.database_proxy.football.FootballFactory.createFranceTeam;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ScenarioScoped
public class UpdateFootballTeamSteps {

    private final List<NationalFootballTeam> footballTeams;
    private final NationalFootballTeamsRepository footballTeamsRepository;
    private final FootballTeamContext footballTeamContext;
    private final NationalFootballTeamService footballTeamService;

    @Inject
    public UpdateFootballTeamSteps(FootballTeamContext footballTeamContext,
                                   NationalFootballTeamsRepository footballTeamsRepository,
                                   NationalFootballTeamService footballTeamService) {
        this.footballTeamContext = footballTeamContext;
        this.footballTeamService = footballTeamService;
        this.footballTeamsRepository = footballTeamsRepository;
        this.footballTeams = footballTeamContext.getFootballTeams();
    }


    @Given("a national football team exists")
    @Transactional
    public void aNationalFootballTeamExists() {
        var france = createFranceTeam();

        footballTeams.add(france);

        footballTeamsRepository.save(france);
    }


    @When("I update the name of the national football team")
    public void iUpdateTheNameOfTheNationalFootballTeam() {
        var existingTeam = footballTeams.get(0);
        var updatedTeam = new NationalFootballTeam()
                .setName("FRAN")
                .setAbbreviatedName(existingTeam.getAbbreviatedName())
                .setAdditionalInfo(existingTeam.getAdditionalInfo());

        footballTeamContext.setResponse(footballTeamService.updateFootballTeam(existingTeam.getName(), updatedTeam));
    }

    @Then("I should be alerted that name cannot be updated")
    public void iShouldBeAlertedThatNameCannotBeUpdated() {
        var response = footballTeamContext.getResponse();
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/national_football_teams/FRANCE"))
                .body("message", containsString("inconsistent primary key values"))
                .body("code", equalTo(400));
    }

    @When("I update the abbreviated name to no value")
    public void iUpdateTheAbbreviatedNameToNoValue() {
        var existingTeam = footballTeams.get(0);
        var updatedTeam = new NationalFootballTeam()
                .setName(existingTeam.getName())
                .setAbbreviatedName(null);

        footballTeamContext.setResponse(footballTeamService.updateFootballTeam(existingTeam.getName(), updatedTeam));
    }

    @Then("I should be alerted that the abbreviated name is a required field")
    public void iShouldBeAlertedThatTheAbbreviatedNameIsARequiredField() {
        var response = footballTeamContext.getResponse();
        response.then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("path", equalTo("/national_football_teams/FRANCE"))
                .body("message", containsString("abbreviated_name cannot be null"))
                .body("code", equalTo(400));
    }

    @When("I update the abbreviated name")
    public void iUpdateTheAbbreviatedName() {
        var footballTeam = footballTeams.get(0)
                .setAbbreviatedName("FRAN");

        footballTeamContext.setResponse(footballTeamService.updateFootballTeam(footballTeam.getName(), footballTeam));
    }

    @Then("I should see the abbreviated name in the national football team")
    public void iShouldSeeTheAbbreviatedNameInTheNationalFootballTeam() {
        var footballTeam = footballTeamsRepository.findTeamByName(footballTeams.get(0).getName());

        assertEquals("FRAN", footballTeam.getAbbreviatedName());
        assertEquals(footballTeams.get(0), footballTeam);
    }

    @When("I update a national football team that does not exist")
    public void iUpdateANationalFootballTeamThatDoesNotExist() {
        var footballTeam = new NationalFootballTeam()
                .setName("MOROCCO")
                .setAbbreviatedName("MOR")
                .setAdditionalInfo(Map.of());
        footballTeamContext.setResponse(footballTeamService.updateFootballTeam(footballTeam.getName(), footballTeam));
    }
}
