package bps.budget

import io.github.nhubbard.konf.Config
import io.github.nhubbard.konf.source.yaml.yaml
import io.github.nhubbard.konf.toValue

fun main() {
    val config: Config = Config()
        .from
        .yaml
        .string(
            """
persistence:
  type: JDBC
  jdbc:
    driver: org.postgresql.Driver
    # this should not be user configurable
    schema: scratch
    dbProvider: postgresql
    port: 5432
    host: localhost
    user: budget
    password: budget
  file:
    budgetName: Default File-Configured Budget
    dataDirectory: ~/.data/bps-budget

budget:
  name: Default Budget
budgetUser:
  defaultLogin: fake@fake.com
""",
        )
    val persistence: PersistenceConfiguration =
        config
            .at("persistence")
            .toValue()
    val user: UserConfiguration =
        config
            .at("user")
            .toValue()

    println(persistence)
    println(user)
}
