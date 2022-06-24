package com.dercio.database_proxy.football;

import com.google.inject.Inject;
import io.cucumber.guice.ScenarioScoped;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;

@ScenarioScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FootballTeamSteps {

    private final FootballTeamContext footballTeamContext;

    @Then("I should be alerted that the national football team does not exist")
    public void iShouldBeAlertedThatTheNationalFootballTeamDoesNotExist() {
        var response = footballTeamContext.getResponse();
        response.then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("path", matchesRegex("/national_football_teams/.*"))
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }
}
