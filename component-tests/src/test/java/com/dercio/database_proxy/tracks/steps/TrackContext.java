package com.dercio.database_proxy.tracks.steps;

import com.dercio.database_proxy.tracks.Track;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ScenarioScoped
public class TrackContext {

    private final List<Track> tracks = new ArrayList<>();
    private Response response;
}
