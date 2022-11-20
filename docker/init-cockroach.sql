CREATE SCHEMA vehicles;

CREATE TABLE vehicles.cars
(
    car_id       bigint primary key not null
        constraint cars_car_id_uindex unique,
    manufacturer varchar(50)        not null,
    doors        bigint default 5,
    last_updated timestamptz default now()
);

INSERT INTO vehicles.cars
VALUES (1, 'BMW', 5),
       (2, 'BMW', 3),
       (3, 'MERCEDES', 5),
       (4, 'MERCEDES', 3);

CREATE TABLE vehicles.wheel
(
    wheel_type varchar(50) primary key not null
);