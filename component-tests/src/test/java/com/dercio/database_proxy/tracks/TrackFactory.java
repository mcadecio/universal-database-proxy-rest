package com.dercio.database_proxy.tracks;

import java.util.UUID;

public class TrackFactory {

    public static final UUID ALBUM_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    private TrackFactory() {
    }

    public static Track createFirstTrack() {
        return new Track(ALBUM_ID, 1, "Moment's Notice", 552000L);
    }

    public static Track createSecondTrack() {
        return new Track(ALBUM_ID, 2, "Locomotion", 427000L);
    }
}
