package com.dercio.database_proxy.genres.steps;

import com.dercio.database_proxy.genres.Genre;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ScenarioScoped
public class GenreContext {

    private final List<Genre> genres = new ArrayList<>();
    private Response response;
}
