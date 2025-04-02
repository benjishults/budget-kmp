# Budget Data Server

## Build application

To build the application, run

```shell
./gradlew server:shadowJar
```

That will put an executable jar in [build/libs/server-all.jar](build/libs/server-all.jar).

## Run Server

You'll need the DB running (see [../cli/README.md](../cli/README.md)).

You'll need a configuration file whose name is the value of the `BPS_BUDGET_SERVER_CONFIG` environment
variable or `~/.config/bps-budget-server/budget-server.yml`. That file should look something
like this:

```yaml
jdbc:
    dbProvider: postgresql
    port: 5432
    host: localhost
    schema: budget
    user: budget
    password: budget

server:
    port: 8080
```

Then hit the `server:run` task with gradle:

```shell
./gradlew server:run -DBPS_BUDGET_LOG_FOLDER=~/.local/share/bps-budget/logs
```

or

```shell
./gradlew server:runShadow -DBPS_BUDGET_LOG_FOLDER=~/.local/share/bps-budget/logs
```

To run it as an application without using gradle, it just

```shell
java -jar build/libs/server-all.jar -DBPS_BUDGET_LOG_FOLDER=~/.local/share/bps-budget/logs
```

Pass in the Java system property `BPS_BUDGET_LOG_FOLDER` to determine the folder where logs will be put.
