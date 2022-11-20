package com.dercio.database_proxy.football;

import com.dercio.database_proxy.repositories.football.NationalFootballTeam;

import java.util.Map;

public class FootballFactory {
    static NationalFootballTeam createInvalidFootbalTeam() {
        return new NationalFootballTeam()
                .setName("ENGLAND")
                .setAbbreviatedName(2021)
                .setAdditionalInfo(Map.of());
    }

    static NationalFootballTeam createFranceTeam() {
        return new NationalFootballTeam()
                .setName("FRANCE")
                .setAbbreviatedName("FR")
                .setAdditionalInfo(Map.of("city", "Pairs"));
    }

    static NationalFootballTeam createItalyTeam() {
        return new NationalFootballTeam()
                .setName("ITALY")
                .setAbbreviatedName("ITA")
                .setAdditionalInfo(Map.of("food", "Pizza"));
    }
}
