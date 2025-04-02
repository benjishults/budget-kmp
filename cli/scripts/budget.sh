if [ "$1" = 'debug' ]; then
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044 -cp ../build/libs/cli-1.0-SNAPSHOT-all.jar -DBPS_BUDGET_LOG_FOLDER=~/.local/share/bps-budget/logs bps.budget.Budget
else
  java -cp ../build/libs/cli-1.0-SNAPSHOT-all.jar -DBPS_BUDGET_LOG_FOLDER=~/.local/share/bps-budget/logs bps.budget.Budget
fi
