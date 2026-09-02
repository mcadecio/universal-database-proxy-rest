package com.dercio.database_proxy.albums.steps;

import com.dercio.database_proxy.albums.Album;
import com.dercio.database_proxy.albums.AlbumFactory;
import com.dercio.database_proxy.albums.AlbumRepository;
import com.dercio.database_proxy.albums.AlbumService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CreateAlbumSteps {

    private final Mapper mapper;
    private final AlbumContext albumContext;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;

    @When("I create a new album")
    public void iCreateANewAlbum() {
        var album = AlbumFactory.createBlueTrain();
        albumContext.getAlbums().add(album);
        albumContext.setResponse(albumService.create(album));
    }

    @Then("the album should be created")
    public void theAlbumShouldBeCreated() {
        assertEquals(201, albumContext.getResponse().statusCode());

        var stored = albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID);

        assertNotNull(stored, "The album should have been persisted");
        assertEquals(AlbumFactory.createBlueTrain(), stored);
    }

    @Then("the album's sets should survive the round trip")
    public void theAlbumsSetsShouldSurviveTheRoundTrip() {
        var stored = albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID);
        var returned = mapper.decode(
                albumService.getById(AlbumFactory.BLUE_TRAIN_ID.toString()).body().asString(), Album.class);

        assertAll(
                () -> assertEquals(Set.of(AlbumFactory.TEST_TAG, "blues"), stored.tags()),
                () -> assertEquals(Set.of(4, 5), stored.ratings()),
                // The JSON array has to come back as a set of the right element type, not strings.
                () -> assertEquals(Set.of(AlbumFactory.TEST_TAG, "blues"), returned.tags()),
                () -> assertEquals(Set.of(4, 5), returned.ratings())
        );
    }

    @Then("the response should point at the new album")
    public void theResponseShouldPointAtTheNewAlbum() {
        // CQL has no RETURNING clause, so the proxy rebuilds this id from the request body.
        var location = albumContext.getResponse().header("location");

        assertTrue(location.endsWith("/albums/" + AlbumFactory.BLUE_TRAIN_ID),
                "Expected the Location header to address the new album but was " + location);

        var followed = albumService.getById(AlbumFactory.BLUE_TRAIN_ID.toString());

        assertEquals(200, followed.statusCode());
        assertEquals("Blue Train", mapper.decode(followed.body().asString(), Album.class).title());
    }
}
