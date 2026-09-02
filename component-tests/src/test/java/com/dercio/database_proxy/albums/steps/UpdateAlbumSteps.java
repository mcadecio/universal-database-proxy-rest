package com.dercio.database_proxy.albums.steps;

import com.dercio.database_proxy.albums.Album;
import com.dercio.database_proxy.albums.AlbumFactory;
import com.dercio.database_proxy.albums.AlbumRepository;
import com.dercio.database_proxy.albums.AlbumService;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateAlbumSteps {

    private final AlbumContext albumContext;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;

    @When("I update the album {string}")
    public void iUpdateTheAlbum(String albumId) {
        var updated = new Album(AlbumFactory.BLUE_TRAIN_ID, "Blue Train (Remaster)", "John Coltrane", 1958, false,
                Set.of("remastered", "blues"), Set.of(5));

        albumContext.setResponse(albumService.update(albumId, updated));
    }

    @Then("I should see the newly updated album")
    public void iShouldSeeTheNewlyUpdatedAlbum() {
        assertEquals(204, albumContext.getResponse().statusCode());

        var stored = albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID);

        assertEquals("Blue Train (Remaster)", stored.title());
        assertEquals(false, stored.inPrint());
    }

    @Then("the album's sets should be replaced")
    public void theAlbumsSetsShouldBeReplaced() {
        var stored = albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID);

        assertAll(
                // An update replaces a set wholesale rather than merging into it.
                () -> assertEquals(Set.of("remastered", "blues"), stored.tags()),
                () -> assertEquals(Set.of(5), stored.ratings())
        );
    }

    @When("I update an album that does not exist")
    public void iUpdateAnAlbumThatDoesNotExist() {
        var ghost = new Album(AlbumFactory.UNKNOWN_ID, "Ghost", "Nobody", 2000, false,
                Set.of("ghostly"), Set.of(1));

        albumContext.setResponse(albumService.update(AlbumFactory.UNKNOWN_ID.toString(), ghost));
    }

    @Then("the album should not be created by the update")
    public void theAlbumShouldNotBeCreatedByTheUpdate() {
        // An INSERT is an upsert in Cassandra, so without the read-before-write this would answer
        // 204 and quietly create the row.
        assertEquals(404, albumContext.getResponse().statusCode());
        assertNull(albumRepository.findById(AlbumFactory.UNKNOWN_ID),
                "Updating a missing album must not create it");
    }
}
