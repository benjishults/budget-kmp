# BPS Budget

## One-Time Setup

### Set up the env for DB

Set the `BPS_BUDGET_DATA_DIR` environment variable to something like `~/data/bps-budget`
and make sure that folder exists.

You'll need to have pulled the postgres image from some container registry:

```shell
docker pull docker.io/postgres:latest
```

### Start Postgres

Run the script to run Postgres in an OSI container.  (I use podman with docker as an alias to podman but docker
works fine, too, if you prefer.)

```shell
./scripts/startDb.sh
```

### Create DB and users

This will create databases, users, and schemas for both production and testing.

```shell
psql -U admin -h localhost -f ./scripts/setupDbAsAdmin.sql
psql -U admin -d budget -h localhost -f ./scripts/setupBudgetSchemasAsAdmin.sql
```

### Build the Application

Building the application involves pulling dependencies from GitHub Packages. So, you'll need GitHub credentials.

The gradle properties `github.actor` (username) and `github.token` (a token with `packages:read` permissions)
need to be set in a file named `gradle.properties` in your `GRADLE_USER_HOME` folder (defaults to `~/.gradle/`).

```properties
github.actor=<your github login>
github.token=<token with packages:read>
```

Alternatively, you can set the environment variables `GITHUB_ACTOR` and `GITHUB_TOKEN`. The latter is nice for the
CI while the former is nice for your IDE.

Then build the application with

```shell
./gradlew shadowJar
```

### Prepare to run for the first time

Create a file named `budget.yml` in your `~/.config/bps-budget` folder.

It should something like this:

```yaml
persistence:
    type: JDBC
    jdbc:
        budgetName: Budget # give your budget a custom name if you want
        dbProvider: postgresql
        port: 5432
        host: localhost # if your DB is running on a different machine, change this to its domain or IP
        schema: budget
        user: budget
        password: budget

budgetUser:
    defaultLogin: fake@fake.com # your email
```

## Run the Application

Currently, this isn't set up to be running in an open environment. The security on the DB is minimal to non-existent.

If you're just running this on your personal machine, and you have some reasonable router connecting you to the
internet (or no connection at all), you should be fine.

Make sure the DB is running. If it isn't running then start it with:

```shell
./scripts/startDb.sh
```

Once the DB is running, start the budget application with:

```shell
./scripts/budget.sh
```

## Backup and Restore Data

Use the `backup.sh` and `restore.sh` scripts to back up and restore your budget data.

## Contributing

### Running Tests

Make sure the DB is running. If it isn't running then start it with:

```shell
./scripts/startDb.sh
```

Run tests with:

```shell
./gradlew test
```

Again, be sure you have the gradle properties `github.actor` (username) and `github.token` (a token with `packages:read`
permissions) need to be set in a file named `gradle.properties` in your `GRADLE_USER_HOME` folder (defaults to
`~/.gradle/`).

```properties
github.actor=<your github login>
github.token=<token with packages:read>
```

## Troubleshooting

To connect to the Postgres DB running in the docker container, do

```shell
psql -U budget -h 127.0.0.1 -d budget
```

When I need to do a data migration, I whip that up here: `bps.budget.persistence.migration.DataMigrations`.

## CI Docker Image

See [Dockerfile](ci/Dockerfile).

Everything you need to know should be in
the [GitHub action that builds and publishes the image](.github/workflows/publish-test-db-container.yml) (builds and
publishes the image) and
the [GitHub action that runs tests](.github/workflows/test.yml) (runs the container).

To test manually, create the image:

```shell
cd ci
docker build -t pg-test .
```

Test it with this

```shell
docker run -e POSTGRES_PASSWORD=test -e POSTGRES_USER=test -e POSTGRES_DB=budget --rm --name pg-test -p 5432:5432 -d pg-test:latest
```

and connect to it to ensure that the `test:test` user has access to two schemas: `test` and `clean_after_test`.

You can look at logs with

```shell
docker logs pg-test
```

The image is published by the [publish-test-db-container.yml](.github/workflows/publish-test-db-container.yml) action.
