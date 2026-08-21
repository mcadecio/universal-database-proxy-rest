package com.dercio.database_proxy.albums.steps;

import com.dercio.database_proxy.albums.Album;
import com.dercio.database_proxy.albums.AlbumFactory;
import com.dercio.database_proxy.albums.AlbumRepository;
import com.dercio.database_proxy.albums.AlbumService;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateAlbumSteps {

    private final AlbumContext albumContext;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;

    @When("I update the album {string}")
    public void iUpdateTheAlbum(String albumId) {
        var updated = new Album(AlbumFactory.BLUE_TRAIN_ID, "Blue Train (Remaster)", "John Coltrane", 1958, false);

        albumContext.setResponse(albumService.update(albumId, updated));
    }

    @Then("I should see the newly updated album")
    public void iShouldSeeTheNewlyUpdatedAlbum() {
        assertEquals(204, albumContext.getResponse().statusCode());

        var stored = albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID);

        assertEquals("Blue Train (Remaster)", stored.title());
        assertEquals(false, stored.inPrint());
    }

    @When("I update an album that does not exist")
    public void iUpdateAnAlbumThatDoesNotExist() {
        var ghost = new Album(AlbumFactory.UNKNOWN_ID, "Ghost", "Nobody", 2000, false);

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
