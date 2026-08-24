package com.dercio.database_proxy.albums;

import java.util.Set;
import java.util.UUID;

public class AlbumFactory {

    // Fixed ids keep failures readable, and every scenario seeds its own rows so the suite can be
    // run repeatedly without wiping the database in between.
    public static final UUID BLUE_TRAIN_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID GIANT_STEPS_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    public static final UUID SAXOPHONE_COLOSSUS_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    public static final UUID UNKNOWN_ID = UUID.fromString("aaaaaaaa-ffff-ffff-ffff-ffffffffffff");

    private AlbumFactory() {
    }

    // Deliberately an artist that does not appear in docker/init-cassandra.cql, so the filtered
    // delete scenario provably only removes rows the scenario itself created.
    public static final String TEST_ARTIST = "Coltrane Quartet";

    // A tag that does not appear in docker/init-cassandra.cql, so the CONTAINS filter scenario only
    // ever matches rows the scenario itself created.
    public static final String TEST_TAG = "hard-bop";

    public static Album createBlueTrain() {
        return new Album(BLUE_TRAIN_ID, "Blue Train", TEST_ARTIST, 1958, true,
                Set.of(TEST_TAG, "blues"), Set.of(4, 5));
    }

    public static Album createGiantSteps() {
        return new Album(GIANT_STEPS_ID, "Giant Steps", TEST_ARTIST, 1960, true,
                Set.of(TEST_TAG), Set.of(5));
    }

    public static Album createSaxophoneColossus() {
        return new Album(SAXOPHONE_COLOSSUS_ID, "Saxophone Colossus", "Sonny Rollins", 1956, false,
                Set.of("calypso"), Set.of(4));
    }
}
