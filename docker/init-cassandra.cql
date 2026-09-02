-- Seed schema for the Cassandra component tests.
-- Each table covers a shape the proxy branches on:
--   albums -> simple partition key, scalar types, and set<text> / set<int> collections
--   tracks -> composite key: partition key + clustering column
--   genres -> every column is part of the primary key (nothing to SET on update)

CREATE KEYSPACE IF NOT EXISTS music
    WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS music.albums
(
    album_id     uuid PRIMARY KEY,
    title        text,
    artist       text,
    release_year int,
    in_print     boolean,
    tags         set<text>,
    ratings      set<int>
);

CREATE TABLE IF NOT EXISTS music.tracks
(
    album_id    uuid,
    track_no    int,
    title       text,
    duration_ms bigint,
    PRIMARY KEY ((album_id), track_no)
);

CREATE TABLE IF NOT EXISTS music.genres
(
    name text PRIMARY KEY
);

INSERT INTO music.albums (album_id, title, artist, release_year, in_print, tags, ratings)
VALUES (11111111-1111-1111-1111-111111111111, 'Kind of Blue', 'Miles Davis', 1959, true,
        {'jazz', 'modal'}, {4, 5});
INSERT INTO music.albums (album_id, title, artist, release_year, in_print, tags, ratings)
VALUES (22222222-2222-2222-2222-222222222222, 'A Love Supreme', 'John Coltrane', 1965, true,
        {'jazz', 'spiritual'}, {5});
INSERT INTO music.albums (album_id, title, artist, release_year, in_print, tags, ratings)
VALUES (33333333-3333-3333-3333-333333333333, 'Bitches Brew', 'Miles Davis', 1970, false,
        {'jazz', 'fusion'}, {3, 4});

INSERT INTO music.tracks (album_id, track_no, title, duration_ms)
VALUES (11111111-1111-1111-1111-111111111111, 1, 'So What', 562000);
INSERT INTO music.tracks (album_id, track_no, title, duration_ms)
VALUES (11111111-1111-1111-1111-111111111111, 2, 'Freddie Freeloader', 589000);
INSERT INTO music.tracks (album_id, track_no, title, duration_ms)
VALUES (22222222-2222-2222-2222-222222222222, 1, 'Acknowledgement', 452000);

INSERT INTO music.genres (name) VALUES ('jazz');
INSERT INTO music.genres (name) VALUES ('fusion');
