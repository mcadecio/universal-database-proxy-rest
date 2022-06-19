package com.dercio.database_proxy.repositories.football;

public interface NationalFootballTeamsRepository {
    NationalFootballTeam findTeamByName(String name);
}
