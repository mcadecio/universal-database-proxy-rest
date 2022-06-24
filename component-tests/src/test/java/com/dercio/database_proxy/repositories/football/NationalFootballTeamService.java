package com.dercio.database_proxy.repositories.football;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

import static io.restassured.RestAssured.given;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class NationalFootballTeamService {

    private final Mapper mapper;
    private static final String BASE_URI = "http://localhost:8000";
    private static final String NATIONAL_FOOTBALL_TEAMS = "/national_football_teams/";

    public Response getFootballTeams() {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(NATIONAL_FOOTBALL_TEAMS)
                .prettyPeek();
    }

    public Response getFootballTeamByName(String name) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(NATIONAL_FOOTBALL_TEAMS + name)
                .prettyPeek();
    }

    public Response getFootballTeamByName(int id) {
        return given()
                .baseUri(BASE_URI)
                .accept(ContentType.JSON)
                .log()
                .all(true)
                .get(NATIONAL_FOOTBALL_TEAMS + id)
                .prettyPeek();
    }

    public Response deleteFootballTeam(String name) {
        return given()
                .baseUri(BASE_URI)
                .log()
                .all(true)
                .delete(NATIONAL_FOOTBALL_TEAMS + name)
                .prettyPeek();
    }

    public Response updateFootballTeam(String originalName, NationalFootballTeam footballTeam) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(footballTeam))
                .log()
                .all(true)
                .put(NATIONAL_FOOTBALL_TEAMS + originalName)
                .prettyPeek();
    }

    public Response createFootballTeam(NationalFootballTeam footbalTeam) {
        return given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(mapper.encode(footbalTeam))
                .log()
                .all(true)
                .post(NATIONAL_FOOTBALL_TEAMS)
                .prettyPeek();
    }
}
