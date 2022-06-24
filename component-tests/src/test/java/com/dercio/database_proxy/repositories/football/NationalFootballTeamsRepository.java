package com.dercio.database_proxy.repositories.football;

public interface NationalFootballTeamsRepository {
    NationalFootballTeam findTeamByName(String name);

    void save(NationalFootballTeam footballTeam);

    void deleteByName(String name);
}
