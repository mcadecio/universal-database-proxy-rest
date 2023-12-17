package com.dercio.database_proxy.students;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentRepository {
    Student findById(@Param("name") String name, @Param("age") int age);

    List<Student> find();

    void save(Student student);

    void deleteByNameAndAge(@Param("name") String name, @Param("age") int age);
}
