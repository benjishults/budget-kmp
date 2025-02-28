# Budget Data Server

## Run Server

You'll need the DB running (see [../cli/README.md](../cli/README.md)).

You'll need a configuration file at `~/.config/bps-budget-server/budget-server.yml`.  That file should look something
like this:

```yaml
jdbc:
  dbProvider: postgresql
  port: 5432
  host: localhost # if your DB is running on a different machine, change this to its domain or IP
  schema: budget
  user: budget
  password: budget

server:
  port: 8080
```

Then hit the `server:run` task with gradle:

```shell
./gradlew server:run
```
