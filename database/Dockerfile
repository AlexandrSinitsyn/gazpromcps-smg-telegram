FROM postgres:15

WORKDIR .

RUN apt-get update && apt-get install -y postgresql-15 postgresql-client-15 postgresql-contrib-15
ADD init.sql /docker-entrypoint-initdb.d/
