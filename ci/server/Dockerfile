FROM eclipse-temurin:21
COPY server-all.jar /tmp/
COPY server-config.yml /tmp/
WORKDIR /tmp
ENV BPS_BUDGET_SERVER_CONFIG=/tmp/server-config.yml
EXPOSE 8080
ENTRYPOINT ["java","-jar","server-all.jar"]
