package com.dercio.database_proxy.students;

import com.dercio.database_proxy.common.RestService;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;

public class StudentService extends RestService {

    private static final String BASE_URI = "http://localhost:8010";
    private static final String STUDENTS = "/students/";

    @Inject
    public StudentService(Mapper mapper) {
        super(BASE_URI, STUDENTS, mapper);
    }
}
