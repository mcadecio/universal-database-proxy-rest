package com.dercio.database_proxy.students;

public class StudentFactory {

    public static Student createAlexStudent() {
        return new Student("Alex", 10, "312312");
    }

    public static Student createDavidStudent() {
        return new Student("David", 38, "80934");
    }

    public static Student createManuelStudent() {
        return new Student("Manuel", 28, "23893");
    }
}
