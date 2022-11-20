package com.dercio.database_proxy.repositories.football;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NationalFootballTeam {
    private String name;
    private Object abbreviatedName;
    private Map<String, Object> additionalInfo;
}
