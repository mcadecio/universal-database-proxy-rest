package com.dercio.database_proxy.tracks.steps;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.tracks.Track;
import com.dercio.database_proxy.tracks.TrackFactory;
import com.dercio.database_proxy.tracks.TrackRepository;
import com.dercio.database_proxy.tracks.TrackService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tracks table has a composite primary key (partition key + clustering column), so these
 * scenarios exercise the {@code {album_id}:{track_no}} path form and the partition-key filter that
 * must not fall back to a cluster scan.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TrackSteps {

    private final Mapper mapper;
    private final TrackContext trackContext;
    private final TrackRepository trackRepository;
    private final TrackService trackService;

    @Given("a list of tracks exists")
    public void aListOfTracksExists() {
        List.of(TrackFactory.createFirstTrack(), TrackFactory.createSecondTrack())
                .forEach(track -> {
                    trackContext.getTracks().add(track);
                    trackRepository.save(track);
                });
    }

    @When("I retrieve all the tracks of the album")
    public void iRetrieveAllTheTracksOfTheAlbum() {
        trackContext.setResponse(trackService.getAll(Map.of("album_id", TrackFactory.ALBUM_ID.toString())));
    }

    @Then("I should see both tracks of that album")
    public void iShouldSeeBothTracksOfThatAlbum() {
        var tracks = mapper.decode(trackContext.getResponse().body().asString(),
                new TypeReference<List<Track>>() {
                });

        assertEquals(2, tracks.size());
        assertTrue(tracks.contains(TrackFactory.createFirstTrack()));
        assertTrue(tracks.contains(TrackFactory.createSecondTrack()));
    }

    @When("I retrieve the track number {int} of the album")
    public void iRetrieveTheTrackNumberOfTheAlbum(int trackNo) {
        trackContext.setResponse(trackService.getById(TrackFactory.ALBUM_ID, trackNo));
    }

    @Then("I should see the track titled {string}")
    public void iShouldSeeTheTrackTitled(String title) {
        var track = mapper.decode(trackContext.getResponse().body().asString(), Track.class);

        assertEquals(title, track.title());
        assertEquals(TrackFactory.ALBUM_ID, track.albumId());
    }

    @When("I update the track number {int} of the album")
    public void iUpdateTheTrackNumberOfTheAlbum(int trackNo) {
        var updated = new Track(TrackFactory.ALBUM_ID, trackNo, "Moment's Notice (Alt Take)", 561000L);

        trackContext.setResponse(trackService.update(TrackFactory.ALBUM_ID, trackNo, updated));
    }

    @Then("I should see the newly updated track")
    public void iShouldSeeTheNewlyUpdatedTrack() {
        assertEquals(204, trackContext.getResponse().statusCode());

        var stored = trackRepository.findById(TrackFactory.ALBUM_ID, 1);

        assertEquals("Moment's Notice (Alt Take)", stored.title());
        assertEquals(561000L, stored.durationMs());
    }

    @When("I delete the track number {int} of the album")
    public void iDeleteTheTrackNumberOfTheAlbum(int trackNo) {
        trackContext.setResponse(trackService.deleteById(TrackFactory.ALBUM_ID, trackNo));
    }

    @Then("the track should be deleted")
    public void theTrackShouldBeDeleted() {
        assertEquals(204, trackContext.getResponse().statusCode());
        assertNull(trackRepository.findById(TrackFactory.ALBUM_ID, 2));
    }

    @Then("I should get a track not found error")
    public void iShouldGetATrackNotFoundError() {
        assertEquals(404, trackContext.getResponse().statusCode());
    }
}
