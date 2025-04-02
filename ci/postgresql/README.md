# PostgreSQL budget test DB

You'll need to have pulled the postgres image from some container registry:

```shell
docker pull docker.io/postgres:17
```

To connect to the Postgres DB running in the docker container, do

```shell
psql -U budget -h 127.0.0.1 -d budget
```

## CI Docker Image

See [Dockerfile](Dockerfile).

Everything you need to know should be in
the [GitHub action that builds and publishes the image](../../.github/workflows/publish-test-db-container.yml)
(builds and publishes the image) and
the [GitHub action that runs tests](../../.github/workflows/test-cli.yml) (runs the container).

### Not needed but for educational purposes

To test manually, create the image:

```shell
cd ../ci/postgresql
docker build -t pg-test .
```

Test it with this

```shell
docker run -e POSTGRES_PASSWORD=test -e POSTGRES_USER=test -e POSTGRES_DB=budget --rm --name pg-test -p 127.0.0.1:5432:5432 -d pg-test:latest
```

and connect to it to ensure that the `test:test` user has access to two schemas: `test` and `clean_after_test`.

You can look at logs with

```shell
docker logs pg-test
```
