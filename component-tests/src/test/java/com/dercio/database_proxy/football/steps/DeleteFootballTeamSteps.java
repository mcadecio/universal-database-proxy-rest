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

import static com.dercio.database_proxy.football.FootballFactory.createFranceTeam;
import static org.junit.jupiter.api.Assertions.assertNull;

@ScenarioScoped
public class DeleteFootballTeamSteps {

    private final List<NationalFootballTeam> footballTeams;
    private final  NationalFootballTeamsRepository footballTeamsRepository;
    private final FootballTeamContext footballTeamContext;
    private final NationalFootballTeamService footballTeamService;

    @Inject
    public DeleteFootballTeamSteps(FootballTeamContext footballTeamContext,
                                   NationalFootballTeamsRepository footballTeamsRepository,
                                   NationalFootballTeamService footballTeamService) {
        this.footballTeamContext = footballTeamContext;
        this.footballTeamService = footballTeamService;
        this.footballTeamsRepository = footballTeamsRepository;
        this.footballTeams = footballTeamContext.getFootballTeams();
    }

    @Given("the national football team I previously created is no longer valid")
    @Transactional
    public void theNationalFootballTeamIPreviouslyCreatedIsNoLongerValid() {
        var footbalTeam = createFranceTeam();
        footballTeams.add(footbalTeam);
        footballTeamsRepository.save(footbalTeam);
    }

    @When("I delete the national football team")
    public void iDeleteTheNationalFootballTeam() {
        footballTeamContext.setResponse(footballTeamService.deleteFootballTeam(footballTeams.get(0).getName()));
    }

    @Then("the national football team should be deleted")
    public void theNationalFootballTeamShouldBeDeleted() {
        var response = footballTeamContext.getResponse();
        response.then()
                .statusCode(204);
        assertNull(footballTeamsRepository.findTeamByName(footballTeams.get(0).getName()));
    }

    @When("I delete a national football team that does not exist")
    public void iDeleteANationalFootballTeamThatDoesNotExist() {
        footballTeamContext.setResponse(footballTeamService.deleteFootballTeam("AMERICA"));
    }
}
