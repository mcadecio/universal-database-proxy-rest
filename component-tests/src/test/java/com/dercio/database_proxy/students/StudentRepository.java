package com.dercio.database_proxy.students;

import org.apache.ibatis.annotations.Param;

public interface StudentRepository {
    Student findById(@Param("name") String name, @Param("age") int age);

    void save(Student student);

    void deleteByNameAndAge(@Param("name") String name, @Param("age") int age);
}
