FROM postgres:17

ENV PGPASSWORD=admin
EXPOSE 5432

COPY init.sql /docker-entrypoint-initdb.d/
