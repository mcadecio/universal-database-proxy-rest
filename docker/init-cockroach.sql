CREATE SCHEMA vehicles;

CREATE TABLE vehicles.cars
(
    manufacturer  varchar(50)        not null,
    car_id        bigint primary key not null
        constraint cars_car_id_uindex unique,
    doors         bigint      default 5,
    last_updated  timestamptz default now(),
    extra_details json        default '{}'
);

INSERT INTO vehicles.cars(car_id, manufacturer, doors)
VALUES (1, 'BMW', 5),
       (2, 'BMW', 3),
       (3, 'MERCEDES', 5),
       (4, 'MERCEDES', 3);

CREATE TABLE vehicles.wheel
(
    wheel_type varchar(50) primary key not null
);

CREATE TABLE students
(
    name    varchar(20),
    age     int,
    phone   varchar(10) not null,
    PRIMARY KEY (name, age)
);
INSERT INTO students(name, age, phone)
VALUES ('Ruben', 10, '38192731'),
       ('Ruben', 12, '423423'),
       ('Carlos', 85, '48239423'),
       ('Steve', 24, '09024232');