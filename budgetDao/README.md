# BPS Budget

## One-Time Setup

### Set up the env for DB

Set the `BPS_BUDGET_DATA_DIR` environment variable to something like `~/data/bps-budget`
and make sure that folder exists.

You'll need to have pulled the postgres image from some container registry:

```shell
docker pull docker.io/postgres:17
```

### Start Postgres

Run the script to run Postgres in an OSI container.  (I use podman with docker as an alias to podman but docker
works fine, too, if you prefer.)

```shell
../cli/scripts/startDb.sh
```

### Create DB and users

This will create databases, users, and schemas for both production and testing.

```shell
psql -U admin -h localhost -f ./scripts/setupDbAsAdmin.sql
psql -U admin -d budget -h localhost -f ./scripts/setupBudgetSchemasAsAdmin.sql
```

## Backup and Restore Data

Use the `backup.sh` and `restore.sh` scripts to back up and restore your budget data.

## Troubleshooting

To connect to the Postgres DB running in the docker container, do

```shell
psql -U budget -h 127.0.0.1 -d budget
```

When I need to do a data migration, I whip that up here: `bps.budget.persistence.migration.DataMigrations`.

## CI Docker Image

See [another README](../ci/postgresql/README.md) for details about building the test DB docker container.
