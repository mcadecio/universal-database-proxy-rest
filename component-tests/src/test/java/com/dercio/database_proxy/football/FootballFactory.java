package com.dercio.database_proxy.football;

import com.dercio.database_proxy.repositories.football.NationalFootballTeam;

public class FootballFactory {
    static NationalFootballTeam createInvalidFootbalTeam() {
        return new NationalFootballTeam()
                .setName("ENGLAND")
                .setAbbreviatedName(2021);
    }

    static NationalFootballTeam createFranceTeam() {
        return new NationalFootballTeam()
                .setName("FRANCE")
                .setAbbreviatedName("FR");
    }

    static NationalFootballTeam createItalyTeam() {
        return new NationalFootballTeam()
                .setName("ITALY")
                .setAbbreviatedName("ITA");
    }
}
