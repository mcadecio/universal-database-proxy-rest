package com.dercio.database_proxy.albums.steps;

import com.dercio.database_proxy.albums.Album;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ScenarioScoped
public class AlbumContext {
    private final List<Album> albums = new ArrayList<>();
    private Response response;
}
