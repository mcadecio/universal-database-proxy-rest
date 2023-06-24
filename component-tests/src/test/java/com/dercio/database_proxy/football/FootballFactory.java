package com.dercio.database_proxy.football;

import java.util.Map;

public class FootballFactory {
    public static NationalFootballTeam createInvalidFootbalTeam() {
        return new NationalFootballTeam()
                .setName("ENGLAND")
                .setAbbreviatedName(2021)
                .setAdditionalInfo(Map.of());
    }

    public static NationalFootballTeam createFranceTeam() {
        return new NationalFootballTeam()
                .setName("FRANCE")
                .setAbbreviatedName("FR")
                .setAdditionalInfo(Map.of("city", "Pairs"));
    }

    public static NationalFootballTeam createItalyTeam() {
        return new NationalFootballTeam()
                .setName("ITALY")
                .setAbbreviatedName("ITA")
                .setAdditionalInfo(Map.of("food", "Pizza"));
    }
}
