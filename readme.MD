# database-proxy

This project is meant to provide a simple restful interface to interact with databases. It reads the selected
tables and generates an OpenApi spec.

Currently, it supports the following databases:

* PostgresDB
* CockroachDB
* Apache Cassandra

## Installation

```shell
mvn clean package
cd universal-database-proxy-rest
java -jar -Dproject.config=cfg/config.json target/universal-database-proxy-rest-*-fat.jar
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
    6. Set `hosts` to the database endpoints in `host:port` form, for example `["db-1:5432", "db-2:5432"]`. Each
       entry can be the endpoint in plain text or the name of an ENVIRONMENT VARIABLE, which may itself hold a comma
       separated list. A single `host` plus `port` is still accepted and is used when `hosts` is absent.
    7. Set the `username` to the database username the server should use to authenticate itself against your chosen
       database. This can be the username in plain text or the name of an ENVIRONMENT VARIABLE.
    9. Set the `password` to the database password the server should use to authenticate itself against your chosen
       database. This can be the password in plain text or an ENVIRONMENT VARIABLE. For example: `admin`
       or `POSTGRES_PASSWORD`
    10. Set the `databaseName` to the name of the database you want the OpenApi to be generated from. This can be the
        name of the database in plain text or the name of an ENVIRONMENT VARIABLE.
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
    6. Set `hosts` to the database endpoints in `host:port` form, for example `["db-1:5432", "db-2:5432"]`. Each
       entry can be the endpoint in plain text or the name of an ENVIRONMENT VARIABLE, which may itself hold a comma
       separated list. A single `host` plus `port` is still accepted and is used when `hosts` is absent.
    7. Set the `username` to the database username the server should use to authenticate itself against your chosen
       database. This can be the username in plain text or the name of an ENVIRONMENT VARIABLE.
    9. Set the `password` to the database password the server should use to authenticate itself against your chosen
       database. This can be the password in plain text or an ENVIRONMENT VARIABLE. For example: `admin`
       or `COCKROACH_PASSWORD`
    10. Set the `databaseName` to the name of the database you want the OpenApi to be generated from. This can be the
        name of the database in plain text or the name of an ENVIRONMENT VARIABLE.
    11. Run the steps in the Installation section and set the `-Dproject.config=` argument to the path of
        the `config.json` file you just create.

### How to run against Apache Cassandra

1. Create a config.json file. You can use the file in `docker/config.json` as a base.
2. Update the `cassandraApi` object.
    1. Set the `enabled` property to `true`
    2. Set the `host` to the address the HTTP Server should attach to. This can be `localhost` or `0.0.0.0`
       or `some.address.com`.
    3. Set the `port` to the port number the HTTP Server should listen for connection on. This can be any valid port
       number.
    4. Set the `openApiFilePath` to where you want the OpenApi file generated to be stored. For
       example: `./cassandraOpenApi.yaml`
    5. Now the `database` object holds configuration regarding the database we are trying base of our API from.
    6. Set `hosts` to the cluster contact points in `host:port` form, for example `["cassandra-1:9042"]`. Each entry
       can be the endpoint in plain text or the name of an ENVIRONMENT VARIABLE.
    7. Set the `username` and `password` the driver should authenticate with. These can be in plain text or the name of
       an ENVIRONMENT VARIABLE, for example `CASSANDRA_PASSWORD`. Leave `username` empty when the cluster runs with the
       default `AllowAllAuthenticator` — sending empty credentials fails the handshake.
    9. Set the `databaseName` to the **keyspace** you want the OpenApi to be generated from. Cassandra has no schemas,
       so the keyspace plays that role and appears as the schema in the generated spec.
    10. Set the `localDatacenter` to the datacenter the driver should route to.
    11. Optionally set `allowFiltering` to `true`. Queries that filter on anything the primary key cannot satisfy
        require a cluster-wide scan; they are rejected with a 400 by default. See the Cassandra notes in
        [AGENTS.md](AGENTS.md) for the other behavioural differences.
    12. Run the steps in the Installation section and set the `-Dproject.config=` argument to the path of
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
