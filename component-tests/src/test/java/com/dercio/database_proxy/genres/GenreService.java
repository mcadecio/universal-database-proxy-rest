package com.dercio.database_proxy.genres;

import com.dercio.database_proxy.common.RestService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;

public class GenreService extends RestService {

    private static final String BASE_URI = "http://localhost:8020";
    private static final String GENRES = "/genres/";

    @Inject
    public GenreService(Mapper mapper) {
        super(BASE_URI, GENRES, mapper);
    }
}
