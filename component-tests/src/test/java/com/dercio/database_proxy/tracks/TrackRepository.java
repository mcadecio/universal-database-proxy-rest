package com.dercio.database_proxy.tracks;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TrackRepository {

    private final CqlSession session;

    public Track findById(UUID albumId, int trackNo) {
        var row = session.execute(session
                        .prepare("SELECT * FROM music.tracks WHERE album_id = ? AND track_no = ?")
                        .bind(albumId, trackNo))
                .one();

        return row == null ? null : toTrack(row);
    }

    public void save(Track track) {
        session.execute(session
                .prepare("INSERT INTO music.tracks (album_id, track_no, title, duration_ms) VALUES (?,?,?,?)")
                .bind(track.albumId(), track.trackNo(), track.title(), track.durationMs()));
    }

    public void deleteById(UUID albumId, int trackNo) {
        session.execute(session
                .prepare("DELETE FROM music.tracks WHERE album_id = ? AND track_no = ?")
                .bind(albumId, trackNo));
    }

    private Track toTrack(Row row) {
        return new Track(
                row.getUuid("album_id"),
                row.isNull("track_no") ? null : row.getInt("track_no"),
                row.getString("title"),
                row.isNull("duration_ms") ? null : row.getLong("duration_ms")
        );
    }
}
