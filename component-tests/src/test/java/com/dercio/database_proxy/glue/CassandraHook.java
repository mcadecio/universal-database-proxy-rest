package com.dercio.database_proxy.glue;

import com.dercio.database_proxy.albums.AlbumRepository;
import com.dercio.database_proxy.albums.steps.AlbumContext;
import com.dercio.database_proxy.genres.GenreRepository;
import com.dercio.database_proxy.genres.steps.GenreContext;
import com.dercio.database_proxy.tracks.TrackRepository;
import com.dercio.database_proxy.tracks.steps.TrackContext;
import com.google.inject.Inject;
import io.cucumber.java.After;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CassandraHook {

    private final AlbumContext albumContext;
    private final AlbumRepository albumRepository;

    private final TrackContext trackContext;
    private final TrackRepository trackRepository;

    private final GenreContext genreContext;
    private final GenreRepository genreRepository;

    @After("@cassandra")
    public void afterScenario() {
        log.info("Cleaning up scenario");

        log.info("Deleting {} albums from scenario", albumContext.getAlbums().size());
        albumContext.getAlbums().forEach(album -> albumRepository.deleteById(album.albumId()));

        log.info("Deleting {} tracks from scenario", trackContext.getTracks().size());
        trackContext.getTracks().forEach(track -> trackRepository.deleteById(track.albumId(), track.trackNo()));

        log.info("Deleting {} genres from scenario", genreContext.getGenres().size());
        genreContext.getGenres().forEach(genre -> genreRepository.deleteByName(genre.name()));
    }
}
