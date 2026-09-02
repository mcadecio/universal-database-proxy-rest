# AGENTS.md

Guidance for AI coding agents working in this repository. Keep it up to date when
build/test commands or module layout change.

## What this project is

**Universal Database Proxy REST** turns a relational database into a REST API. At startup it
introspects the configured database's schema (tables, columns, primary keys), generates an
OpenAPI 3 spec, and serves auto-generated CRUD endpoints (`GET`/`POST`/`PUT`/`DELETE`) for each
table — no per-table code required.

Supported databases: **PostgreSQL**, **CockroachDB** and **Apache Cassandra**. Cockroach is spoken
to over the Postgres wire protocol, so both share the `postgres` code. Cassandra is not relational
and has its own `cassandra` package — see [Cassandra notes](#cassandra-notes) for the behavioural
differences that fall out of CQL.

**Tech stack:** Java 25, Maven (multi-module), [Eclipse Vert.x](https://vertx.io) (async HTTP +
`JDBCClient`, plus `vertx-cassandra-client` for Cassandra), Google Guice (DI), Lombok, Log4j2,
Swagger/OpenAPI. Tests: JUnit 5, Mockito, Cucumber, REST-assured, MyBatis, DataStax driver.

## Module layout

This is a Maven reactor with three modules (see root `pom.xml`):

| Module | Path | Purpose |
| --- | --- | --- |
| `common` | `common/` | Shared building blocks (config binding, JSON `Mapper`, Guice helpers). |
| `universal-database-proxy-rest` | `universal-database-proxy-rest/` | The application: schema introspection, OpenAPI generation, REST handlers, DB access. |
| `component-tests` | `component-tests/` | Cucumber end-to-end tests that drive a **running** proxy against live databases. |

Key packages in the main module (`universal-database-proxy-rest/src/main/java/com/dercio/database_proxy/`):
- `restapi/` — HTTP handlers (`RestApiHandler`) and the verticles that host the API.
- `postgres/` — DB access: `PgTableFinder` (schema introspection), `PgObjectFinder`/`PgObjectInserter`/`PgObjectDeleter` (SQL), `PgTableMetadata`, `type/PgType` (type coercion).
- `cassandra/` — the same roles for CQL, over `vertx-cassandra-client` (DataStax driver) instead of the JDBC client: `CassandraTableFinder`, `CassandraObjectFinder`/`CassandraObjectInserter`/`CassandraObjectDeleter`, `CassandraTableMetadata`, `type/CassandraType`.
- `openapi/` — builds the OpenAPI document from `TableMetadata`.
- `common/database/` — `TableMetadata`, `ColumnMetadata`, `Repository` interface, `TableRequest`.

## Build

```bash
mvn clean package            # builds all modules + the runnable fat jar
```

The runnable artifact is `universal-database-proxy-rest/target/universal-database-proxy-rest-*-fat.jar`
(the version is derived from git tags via jgitver, so don't hardcode it).

CI on JDK 25 (`.github/workflows/`):
- `maven.yml` — `mvn -B package` (unit tests) on push/PR to `master`.
- `functional-tests.yml` — starts the databases with compose, seeds Cassandra, runs the fat jar, then
  runs the Cucumber suite. Same flow as the manual steps below.
- `publish-docker.yml` — pushes `mcadecio/universal-database-proxy-rest`; `:latest` on master, plus
  `:<version>` on a `v*` tag. Needs the `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets.

All three check out with `fetch-depth: 0` because jgitver derives the version from git tags. The
publish workflow must `clean` first: the Dockerfile copies a single `*-fat.jar` and fails if a stale
jar from another version is present.

## Run the application

```bash
cd universal-database-proxy-rest
java -jar -Dproject.config=cfg/config.json target/universal-database-proxy-rest-*-fat.jar
```

`-Dproject.config` points at a JSON config (schema: `cfg/config.schema.json`). Each of
`postgresApi` / `cockroachApi` / `cassandraApi` can be independently enabled. Every one of
those keys must be **present** in the config file even when disabled — `ConfigurationBinder` only
logs a missing key and binds nothing, which later surfaces as an opaque Guice `ProvisionException`
at deploy time. Config string values for
`hosts`/`host`/`username`/`password`/`databaseName` are resolved as **environment-variable names
first, falling back to the literal string** — e.g. `"password": "POSTGRES_PASSWORD"` reads
`$POSTGRES_PASSWORD`. Database endpoints are a `hosts` list of `host:port` (a single `host` + `port`
still works as a fallback); a `hosts` entry may itself expand to a comma separated list, which is how
one config file serves both compose and host runs. See `readme.MD` for the full walkthrough.

## Tests

### Unit tests (fast, no database)

Live in `universal-database-proxy-rest/src/test/...`. Pure logic + Mockito; run in the normal
`test` phase. Always include `-am` so the `common` dependency is built first:

```bash
mvn -pl universal-database-proxy-rest -am test          # main-module unit tests
mvn test                                                # whole reactor (unit tests only)
```

There is **no assertion library beyond JUnit 5 + Mockito** — match that style. Existing examples:
`PgTypeTest`, `TableMetadataTest`, `PgTableMetadataTest`, `PgObjectInserterTest`,
`RestApiHandlerTest`, `RestApiVerticleSelectorTest`, `ErrorFactoryTest`, `OpenApiCreatorTest`,
`PgModuleTest`, `CockroachModuleTest`, `CassandraTypeTest`, `CassandraTableMetadataTest`,
`CassandraTableFinderTest`, `CassandraObjectFinderTest`, `CassandraObjectInserterTest`,
`CassandraObjectDeleterTest`, `CassandraModuleTest`.

### Component tests (end-to-end, need Docker + a running proxy)

The Cucumber suite lives in `component-tests/` (features under
`component-tests/src/test/resources/features/`, glue under `.../src/test/java/...`). It uses
REST-assured to hit a **running** proxy and MyBatis to seed/verify data directly in the DBs — except
for Cassandra, which has no JDBC driver and so seeds through the DataStax `CqlSession` directly
(`glue/CassandraTestModule`, a plain module rather than one of the MyBatis `PrivateModule`s).

> The suite is **deliberately excluded from the normal build** — the parent `pom.xml` surefire
> config excludes `**/TestRunner.java`, because it needs live services. So `mvn test` does **not**
> run it. Run it explicitly (see below).

**How the pieces connect:**
- Postgres-backed API is served on **`localhost:8000`** (tables: `budgets`, `national_football_teams`).
- Cockroach-backed API is served on **`localhost:8010`** (tables: `cars`, `wheel`, `students`).
- Cassandra-backed API is served on **`localhost:8020`** (keyspace `music`, tables: `albums` — which
  carries the `set<text>` / `set<int>` columns — `tracks`, `genres`).
- MyBatis (test setup/teardown) connects directly to **`localhost:5432`** (Postgres) and
  **`localhost:26257`** (Cockroach) — see `component-tests/src/test/resources/batis/*/*.properties`.
  The Cassandra tests connect to **`localhost:9042`** — see
  `component-tests/src/test/resources/cassandra/cassandra.properties`.

**Step-by-step:**

1. Start the three databases (do **not** start the bundled `database-proxy` service — you want to
   test *your* build, not the published image). Use a clean volume to avoid stale data:
   ```bash
   cd docker
   docker compose down -v            # wipe any old data first
   docker compose up -d crdb postgres cassandra
   ```
   Postgres and Cockroach seed themselves from `docker/init-postgres.sql` and
   `docker/init-cockroach.sql`. **Cassandra does not** — its image has no
   `/docker-entrypoint-initdb.d`, so a one-shot companion service pipes `docker/init-cassandra.cql`
   through `cqlsh`. It waits on the healthcheck, and Cassandra takes ~60s to become healthy:
   ```bash
   docker compose run --rm cassandra-init
   ```
   The script uses `CREATE TABLE IF NOT EXISTS`, so it will **not** pick up a schema change against a
   container that already has the keyspace. Either `docker compose down -v` first, or
   `cqlsh -e "DROP KEYSPACE music"` and re-run it.

2. Build the fat jar (`mvn clean package`) and run the API against the containers. `docker/config.json`
   takes its endpoints from environment variables, so the same file works inside compose and on the
   host — only the exported values differ:
   ```bash
   POSTGRES_HOSTS=localhost:5432 POSTGRES_PASSWORD=admin \
   CRB_HOSTS=localhost:26257 CRB_PASSWORD= \
   CASSANDRA_HOSTS=localhost:9042 CASSANDRA_PASSWORD=cassandra \
     java -jar -Dproject.config=docker/config.json \
     universal-database-proxy-rest/target/universal-database-proxy-rest-*-fat.jar
   ```
   Wait until `curl -sf localhost:8000/budgets`, `curl -sf localhost:8010/cars` and
   `curl -sf localhost:8020/albums` all succeed.

3. Run the Cucumber suite (override the surefire exclusion). `failIfNoSpecifiedTests=false` keeps
   the empty `common` module from aborting the reactor:
   ```bash
   mvn -pl component-tests -am test \
     -Dtest=TestRunner -Dsurefire.failIfNoSpecifiedTests=false
   ```

4. Tear down when done:
   ```bash
   cd docker && docker compose down -v
   ```

In IntelliJ, `.run/TestRunner.run.xml` runs the same suite and `.run/Application.run.xml` runs the app.

## Cassandra notes

CQL is not SQL, and four assumptions the Postgres code makes simply do not hold. The behaviours
below are **deliberate** — they are what makes the Cassandra API match the Postgres one from the
outside. Don't "simplify" them away.

| Postgres does | Cassandra does instead | Where |
| --- | --- | --- |
| `information_schema` introspection | `system_schema.tables` + `system_schema.columns`; materialized views are filtered out because they cannot be written through | `CassandraTableFinder` |
| `INSERT ... RETURNING pk` for the `Location` header | No `RETURNING`; the id is rebuilt from the primary key values in the request body | `CassandraObjectInserter.create` |
| `rowCount()` decides 204 vs 404 | Writes report no row count **and `INSERT` is an upsert**, so `PUT`/`DELETE` do a read-before-write; without it a `PUT` on a missing row would silently create it | `CassandraObjectFinder.existsByPrimaryKey` |
| `WHERE any_column = ?` | Needs `ALLOW FILTERING` unless the whole partition key is restricted and the clustering columns form a contiguous prefix. Gated by the `allowFiltering` config flag (default **`false`**); when off, such a query is a 400 | `CassandraTableMetadata.requiresAllowFiltering` |
| `DELETE FROM t` with no `WHERE` | Rejected by CQL, so a collection delete becomes `TRUNCATE`. It reports no count, so **`DELETE /table` always answers 204** — an already-empty table is not a 404 here. A *filtered* delete selects the matching rows then deletes each by primary key | `CassandraObjectDeleter` |

Three further traps:

- **The DataStax driver binds by exact Java type** and rejects mismatches where JDBC would coerce.
  A `bigint` column must receive a `Long`, not the `Integer` that JSON hands you; a `uuid` column a
  `UUID`, not a `String`. All of that lives in `CassandraType` — add new types there, not at call sites.
- **`set<T>` is handled outside the `CassandraType` enum**, because a parameterized type cannot be an
  enum constant — see `CassandraType.setElementType`, which also unwraps `frozen<set<T>>`. Sets
  surface as OpenAPI `array` with a typed `items` (a spec that omits `items` fails validation when
  the router loads it), bind as a `java.util.Set` of driver-typed elements, and read back as a
  `JsonArray`. They are filtered by **membership**: `?tags=jazz` generates `tags CONTAINS ?`, which
  binds one *element* rather than a set and always requires `ALLOW FILTERING`. Their query parameter
  is therefore declared as a scalar of the element type — an array-typed query parameter would also
  break `RestApiHandler`, which flattens the query string to one value per name.
- **`localDatacenter` is mandatory.** Startup fails without it rather than guessing `datacenter1`.
- **Tables whose columns are all part of the primary key** have nothing to `SET`, and CQL cannot
  assign a key column to itself the way `PgObjectInserter` does. The update issues no statement at
  all and answers from the existence check (`music.genres` covers this).

**Known gaps** (fine to add later, currently out of scope): `list` and `map` collections, UDTs and
`duration` fall through to OpenAPI `ANY` and are passed through untouched (`set` **is** supported —
see the trap above); a set of an unsupported element type becomes an array of `ANY`; updating a set
replaces it wholesale rather than supporting CQL's `+`/`-` append and remove; no counter columns, TTL
or consistency-level tuning; no materialized views; the filter planner is not aware of secondary or
collection indexes, so an indexed column still gets `ALLOW FILTERING`.

## Conventions & gotchas

- **Always pass `-am`** when building a single module with `-pl`; otherwise Maven can't resolve the
  `common`/parent artifacts and fails with "Could not find artifact ... common".
- SQL/CQL is built with a **column-name whitelist** (filters are checked against real table columns)
  and **bound values** — preserve this; never string-concatenate user input into a query. See
  `PgObjectFinder` / `PgObjectDeleter` and `CassandraObjectFinder`.
- `UPDATE` statements set **non-primary-key columns only** (`PgObjectInserter.generateSetClause`);
  tables whose columns are all part of the PK fall back to a valid no-op `SET pk = pk` (Postgres) or
  skip the statement entirely (Cassandra — see [Cassandra notes](#cassandra-notes)).
- Component tests assume a **clean database**. Stale rows (e.g. from a reused container volume)
  cause confusing failures — `docker compose down -v` before a run. The `@cassandra` scenarios are
  the exception: they seed their own rows and clean up in `CassandraHook`, so they can be run
  repeatedly without a wipe. Note that 12 of the older feature files are missing their
  `@postgres`/`@cockroach` tag, so their hooks never fire — don't copy that when adding features.
- Lombok is used heavily (`@RequiredArgsConstructor(onConstructor_ = @Inject)`, `@Getter`, etc.);
  ensure annotation processing is enabled in your toolchain.
