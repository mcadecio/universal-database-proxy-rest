# database-proxy

This project is meant to provide a simple restful interface to interact with databases. It features the ability to
act as a proxy between any servers over a TCP connection. Also, it has the ability to read selected tables and
generate an OpenApi spec.

Currently, it supports the following databases:

* PostgresDB
* CockroachDB

## Installation

```shell
mvn clean package
cd universal-database-proxy-rest
java -jar -Dproject.config=cfg/config.json target/universal-database-proxy-rest-0.0.5-fat.jar
```

## Usage

### How to run against a PostgresDB

1. Create a config.json file. You can use the file in `docker/config.json` as a base.
2. Update the `postgresApi` object.
    1. Set the `enabled` property to `true`
    2. Set the `host` to the address the HTTP Server should attach to. This can be `localhost` or `0.0.0.0`
       or `some.address.com`.
    3. Set the `port` to the port number the HTTP Server should listen for connection on. This can be any valid port
       number.
    4. Set the `openApiFilePath` to where you want the OpenApi file generated to be stored. For
       example: `./pgOpenApi.yaml`
    5. Now the `database` object holds configuration regarding the database we are trying base of our API from.
    6. Set the `host` to the address of the database. For example: `localhost` or `0.0.0.0` or `some.address.com`.
    7. Set the `port` to the port number the database is listening for connections on. This can be any valid port
       number.
    8. Set the `username` to the database username the server should use to authenticate itself against your chosen
       database.
    9. Set the `password` to the database password the server should use to authenticate itself against your chosen
       database. This can be the password in plain text or an ENVIRONMENT VARIABLE. For example: `admin`
       or `POSTGRES_PASSWORD`
    10. Set the `databaseName` to the name of the database you want the OpenApi to be generated from.
    11. Run the steps in the Installation section and set the `-Dproject.config=` argument to the path of
        the `config.json` file you just create.

### How to run against CockroachDB

1. Create a config.json file. You can use the file in `docker/config.json` as a base.
2. Update the `cockroachApi` object.
    1. Set the `enabled` property to `true`
    2. Set the `host` to the address the HTTP Server should attach to. This can be `localhost` or `0.0.0.0`
       or `some.address.com`.
    3. Set the `port` to the port number the HTTP Server should listen for connection on. This can be any valid port
       number.
    4. Set the `openApiFilePath` to where you want the OpenApi file generated to be stored. For
       example: `./crbOpenApi.yaml`
    5. Now the `database` object holds configuration regarding the database we are trying base of our API from.
    6. Set the `host` to the address of the database. For example: `localhost` or `0.0.0.0` or `some.address.com`.
    7. Set the `port` to the port number the database is listening for connections on. This can be any valid port
       number.
    8. Set the `username` to the database username the server should use to authenticate itself against your chosen
       database.
    9. Set the `password` to the database password the server should use to authenticate itself against your chosen
       database. This can be the password in plain text or an ENVIRONMENT VARIABLE. For example: `admin`
       or `COCKROACH_PASSWORD`
    10. Set the `databaseName` to the name of the database you want the OpenApi to be generated from.
    11. Run the steps in the Installation section and set the `-Dproject.config=` argument to the path of
        the `config.json` file you just create.

### Docker

There is a working example in the `docker` folder on how to integrate in a docker-compose file.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

```text
MIT License

Copyright (c) 2021 Dercio Daio

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
