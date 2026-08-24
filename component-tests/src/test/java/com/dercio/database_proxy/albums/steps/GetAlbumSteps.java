package com.dercio.database_proxy.albums.steps;

import com.dercio.database_proxy.albums.Album;
import com.dercio.database_proxy.albums.AlbumFactory;
import com.dercio.database_proxy.albums.AlbumRepository;
import com.dercio.database_proxy.albums.AlbumService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GetAlbumSteps {

    private final Mapper mapper;
    private final AlbumContext albumContext;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;

    @Given("a list of albums exists")
    public void aListOfAlbumsExists() {
        List.of(
                AlbumFactory.createBlueTrain(),
                AlbumFactory.createGiantSteps(),
                AlbumFactory.createSaxophoneColossus()
        ).forEach(album -> {
            albumContext.getAlbums().add(album);
            albumRepository.save(album);
        });
    }

    @When("I retrieve all the albums")
    public void iRetrieveAllTheAlbums() {
        albumContext.setResponse(albumService.getAll());
    }

    @Then("I should see the albums I created")
    public void iShouldSeeTheAlbumsICreated() {
        var albums = decodeAlbums();

        assertTrue(albums.containsAll(albumContext.getAlbums()),
                "Every seeded album should be present in the response");
    }

    @When("I retrieve all the albums by artist {string}")
    public void iRetrieveAllTheAlbumsByArtist(String artist) {
        albumContext.setResponse(albumService.getAll(Map.of("artist", artist)));
    }

    @Then("I should see only albums by {string}")
    public void iShouldSeeOnlyAlbumsBy(String artist) {
        var albums = decodeAlbums();

        assertTrue(albums.stream().allMatch(album -> artist.equals(album.artist())),
                "Filtering on a non-key column should still only return matching rows");
        assertTrue(albums.contains(AlbumFactory.createBlueTrain()));
        assertTrue(albums.contains(AlbumFactory.createGiantSteps()));
    }

    @When("I retrieve the album {string}")
    public void iRetrieveTheAlbum(String albumId) {
        albumContext.setResponse(albumService.getById(albumId));
    }

    @Then("I should see the album titled {string}")
    public void iShouldSeeTheAlbumTitled(String title) {
        var album = mapper.decode(albumContext.getResponse().body().asString(), Album.class);

        assertEquals(title, album.title());
        assertEquals(AlbumFactory.BLUE_TRAIN_ID, album.albumId());
    }

    @Then("I should see the album's tags and ratings")
    public void iShouldSeeTheAlbumsTagsAndRatings() {
        var album = mapper.decode(albumContext.getResponse().body().asString(), Album.class);

        assertAll(
                () -> assertEquals(Set.of(AlbumFactory.TEST_TAG, "blues"), album.tags()),
                () -> assertEquals(Set.of(4, 5), album.ratings())
        );
    }

    @When("I retrieve all the albums tagged {string}")
    public void iRetrieveAllTheAlbumsTagged(String tag) {
        albumContext.setResponse(albumService.getAll(Map.of("tags", tag)));
    }

    @Then("I should see only albums tagged {string}")
    public void iShouldSeeOnlyAlbumsTagged(String tag) {
        var albums = decodeAlbums();

        assertAll(
                // Membership, not equality - "Blue Train" carries a second tag as well.
                () -> assertTrue(albums.stream().allMatch(album -> album.tags().contains(tag))),
                () -> assertTrue(albums.contains(AlbumFactory.createBlueTrain())),
                () -> assertTrue(albums.contains(AlbumFactory.createGiantSteps())),
                () -> assertFalse(albums.contains(AlbumFactory.createSaxophoneColossus()))
        );
    }

    @When("I retrieve all the albums rated {int}")
    public void iRetrieveAllTheAlbumsRated(int rating) {
        albumContext.setResponse(albumService.getAll(Map.of("ratings", rating)));
    }

    @Then("I should see the album rated {int} but not the others")
    public void iShouldSeeTheAlbumRatedButNotTheOthers(int rating) {
        var albums = decodeAlbums();

        assertAll(
                () -> assertTrue(albums.stream().allMatch(album -> album.ratings().contains(rating))),
                () -> assertTrue(albums.contains(AlbumFactory.createBlueTrain()))
        );
    }

    @Then("I should get an album not found error")
    public void iShouldGetAnAlbumNotFoundError() {
        albumContext.getResponse().then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("message", equalTo("Not Found"))
                .body("code", equalTo(404));
    }

    private List<Album> decodeAlbums() {
        return mapper.decode(albumContext.getResponse().body().asString(), new TypeReference<List<Album>>() {
        });
    }
}
