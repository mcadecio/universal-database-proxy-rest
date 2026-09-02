package com.dercio.database_proxy.tracks;

import com.dercio.database_proxy.common.RestService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;

public class TrackService extends RestService {

    private static final String BASE_URI = "http://localhost:8020";
    private static final String TRACKS = "/tracks/";

    @Inject
    public TrackService(Mapper mapper) {
        super(BASE_URI, TRACKS, mapper);
    }
}
