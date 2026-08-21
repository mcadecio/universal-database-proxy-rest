package com.dercio.database_proxy.albums;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Cassandra has no JDBC driver, so this talks to the cluster through the DataStax driver instead of
 * MyBatis. The method shape mirrors the MyBatis repositories used by the other suites.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AlbumRepository {

    private final CqlSession session;

    public List<Album> find() {
        return session.execute("SELECT * FROM music.albums")
                .all()
                .stream()
                .map(this::toAlbum)
                .toList();
    }

    public Album findById(UUID albumId) {
        var row = session.execute(
                        session.prepare("SELECT * FROM music.albums WHERE album_id = ?").bind(albumId))
                .one();

        return row == null ? null : toAlbum(row);
    }

    public void save(Album album) {
        session.execute(session
                .prepare("INSERT INTO music.albums (album_id, title, artist, release_year, in_print) VALUES (?,?,?,?,?)")
                .bind(album.albumId(), album.title(), album.artist(), album.releaseYear(), album.inPrint()));
    }

    public void deleteById(UUID albumId) {
        session.execute(session.prepare("DELETE FROM music.albums WHERE album_id = ?").bind(albumId));
    }

    private Album toAlbum(Row row) {
        return new Album(
                row.getUuid("album_id"),
                row.getString("title"),
                row.getString("artist"),
                row.isNull("release_year") ? null : row.getInt("release_year"),
                row.isNull("in_print") ? null : row.getBoolean("in_print")
        );
    }
}
