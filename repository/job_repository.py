from typing import List, Optional

from repository.database import *
from dto.job import *


# row := (id, master, title, timestamp)
# row := (id, job_id, count, timestamp)

def to_job(row) -> Job:
    return Job(row[0], row[1], row[2], row[3])


def to_cjob(row) -> CompletedJob:
    job = find_by_id(row[1])

    if job is None:
        job = Job(-1, 'unknown or deleted', 'unknown or deleted', datetime.now())

    return CompletedJob(row[0], job, row[2], row[3])


def collect_daily() -> List[CompletedJob]:
    return run_query('SELECT * FROM completed ;') \
        (lambda rows: [to_cjob(row) for row in rows])


def find_all() -> List[Job]:
    return run_query('SELECT * FROM job ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_by_master(master: str) -> List[Job]:
    return run_query(f'SELECT * FROM job where master=%(m)s ;', m=master) \
        (lambda rows: [to_job(row) for row in rows])


def find_by_id(job_id: int) -> Optional[Job]:
    def find(rows):
        for row in rows:
            return to_job(row)
        return None

    return run_query('SELECT * FROM job where id=%(ji)s ;', ji=job_id)(find)


def find_by_params(master: str, title: str) -> Optional[Job]:
    def find(rows):
        for row in rows:
            return to_job(row)
        return None

    return run_query(
        f'SELECT * FROM job where master=%(m)s AND title=%(t)s ;',
        m=master, t=title)(find)


def save_job(job: Job):
    run_query(f'INSERT INTO job (master, title) VALUES (%(m)s, %(t)s) ;',
              m=job.master, t=job.title)(id)


def save_cjob(completed_job: CompletedJob):
    run_query(f'INSERT INTO completed (job_id, count) VALUES (%(ji)s, %(c)s) ;',
              ji=completed_job.job.id, c=completed_job.count)(id)


def drop_table():
    run_query(f'TRUNCATE TABLE job CASCADE ;')(id)
    run_query(f'TRUNCATE TABLE completed CASCADE ;')(id)
