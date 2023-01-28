import os
import psycopg2

db_name = os.environ['POSTGRES_DB']
db_user = os.environ['POSTGRES_USER']
db_password = os.environ['POSTGRES_PASSWORD']

connection = psycopg2.connect(dbname=db_name, user=db_user, password=db_password,
                              host='127.0.0.1', port=5432)


def run_query(query: str):
    def inner(fn):
        with connection.cursor() as cursor:
            cursor.execute(query)
            return fn(cursor)
    return inner
