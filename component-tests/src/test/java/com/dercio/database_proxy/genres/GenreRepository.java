package com.dercio.database_proxy.genres;

import com.datastax.oss.driver.api.core.CqlSession;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GenreRepository {

    private final CqlSession session;

    public List<Genre> find() {
        return session.execute("SELECT * FROM music.genres")
                .all()
                .stream()
                .map(row -> new Genre(row.getString("name")))
                .toList();
    }

    public Genre findByName(String name) {
        var row = session.execute(
                        session.prepare("SELECT * FROM music.genres WHERE name = ?").bind(name))
                .one();

        return row == null ? null : new Genre(row.getString("name"));
    }

    public void save(Genre genre) {
        session.execute(session.prepare("INSERT INTO music.genres (name) VALUES (?)").bind(genre.name()));
    }

    public void deleteByName(String name) {
        session.execute(session.prepare("DELETE FROM music.genres WHERE name = ?").bind(name));
    }
}
