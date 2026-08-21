package com.dercio.database_proxy.albums.steps;

import com.dercio.database_proxy.albums.AlbumFactory;
import com.dercio.database_proxy.albums.AlbumRepository;
import com.dercio.database_proxy.albums.AlbumService;
import com.google.inject.Inject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DeleteAlbumSteps {

    private final AlbumContext albumContext;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;

    @When("I delete the album {string}")
    public void iDeleteTheAlbum(String albumId) {
        albumContext.setResponse(albumService.deleteById(albumId));
    }

    @Then("the album should be deleted")
    public void theAlbumShouldBeDeleted() {
        assertEquals(204, albumContext.getResponse().statusCode());
        assertNull(albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID));
    }

    @Then("I should get an album not found error for the delete")
    public void iShouldGetAnAlbumNotFoundErrorForTheDelete() {
        // A DELETE by primary key always succeeds in CQL - the 404 comes from the read-before-write.
        assertEquals(404, albumContext.getResponse().statusCode());
    }

    @When("I delete all the albums by artist {string}")
    public void iDeleteAllTheAlbumsByArtist(String artist) {
        albumContext.setResponse(albumService.delete(Map.of("artist", artist)));
    }

    @Then("only the albums by {string} should remain")
    public void onlyTheAlbumsByShouldRemain(String artist) {
        assertEquals(204, albumContext.getResponse().statusCode());

        assertNull(albumRepository.findById(AlbumFactory.BLUE_TRAIN_ID));
        assertNull(albumRepository.findById(AlbumFactory.GIANT_STEPS_ID));
        assertNotNull(albumRepository.findById(AlbumFactory.SAXOPHONE_COLOSSUS_ID),
                "A filtered delete must not touch rows that do not match");
    }
}
