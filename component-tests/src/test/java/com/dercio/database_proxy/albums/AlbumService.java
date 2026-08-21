package com.dercio.database_proxy.albums;

import com.dercio.database_proxy.common.RestService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;

public class AlbumService extends RestService {

    private static final String BASE_URI = "http://localhost:8020";
    private static final String ALBUMS = "/albums/";

    @Inject
    public AlbumService(Mapper mapper) {
        super(BASE_URI, ALBUMS, mapper);
    }
}
