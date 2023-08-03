FROM arm64v8/mysql:8.0-oracle

COPY ./my.cnf /etc/mysql/my.cnf
RUN chmod 644 /etc/mysql/my.cnf
COPY ./sql_init /docker-entrypoint-initdb.d