CREATE SCHEMA money;

CREATE TABLE money.budgets
(
    id            bigserial
        primary key,
    year          integer not null,
    month         integer not null,
    income        numeric not null,
    food          numeric not null,
    rent          numeric not null,
    savings       numeric not null,
    discretionary numeric not null,
    user_id       varchar(35) default '62486028383b0e006fbd161d'::character varying,
    created       timestamp   default now()
);

INSERT INTO money.budgets (id, year, month, income, food, rent, savings, discretionary, user_id)
VALUES (1, 2018, 1, 400, 100, 300, 0, 10, '62486028383b0e006fbd161d'),
       (2, 2018, 2, 400, 100, 300, 0, 10, '62486028383b0e006fbd161d'),
       (3, 2018, 3, 400, 100, 300, 0, 10, '62486028383b0e006fbd161d');

CREATE SCHEMA football;

CREATE TABLE football.national_football_teams
(
    name             varchar(50) primary key not null,
    abbreviated_name varchar(10)             not null
);

INSERT INTO football.national_football_teams (name, abbreviated_name)
VALUES ('PORTUGAL', 'POR'),
       ('SWITZERLAND', 'SUI')