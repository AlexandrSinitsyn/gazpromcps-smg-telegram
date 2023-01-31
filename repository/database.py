import os
import psycopg2

db_name = os.environ['POSTGRES_DB']
db_user = os.environ['POSTGRES_USER']
db_password = os.environ['POSTGRES_PASSWORD']
db_host = os.environ['POSTGRES_HOST']

connection = psycopg2.connect(dbname=db_name, user=db_user, password=db_password,
                              host=db_host, port=5432)


def run_query(query: str, **kwargs):
    def inner(fn):
        with connection.cursor() as cursor:
            cursor.execute(query, kwargs)
            return fn(cursor)
    return inner
