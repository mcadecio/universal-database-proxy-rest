package com.dercio.database_proxy.students;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Student(String name, int age, String phone) {
}
