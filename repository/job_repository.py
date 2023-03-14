from typing import List, Optional

from repository.database import *
from dto.job import *


# row := (id, master, title, measurement, is_active, timestamp)
# row := (id, job_id, count, timestamp)

def to_job(row) -> Job:
    return Job(row[0], row[1], row[2], row[3], row[4], row[5])


def to_cjob(row) -> CompletedJob:
    job = find_by_id(row[1])

    if job is None:
        job = Job.fake()

    return CompletedJob(row[0], job, row[2], row[3])


def collect_daily(start: datetime) -> List[CompletedJob]:
    return run_query('SELECT completed.id, completed.job_id, completed.count, completed.timestamp FROM completed '
                     'JOIN job ON completed.job_id = job.id WHERE job.is_active AND completed.timestamp >= %(t)s ;',
                     t=str(start))(lambda rows: [to_cjob(row) for row in rows])


def find_all() -> List[Job]:
    return run_query('SELECT * FROM job ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_active() -> List[Job]:
    return run_query('SELECT * FROM job WHERE is_active ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_by_master(master: str) -> List[Job]:
    return run_query(f'SELECT * FROM job where master=%(m)s ;', m=master) \
        (lambda rows: [to_job(row) for row in rows])


def find_active_by_master(master: str) -> List[Job]:
    return run_query(f'SELECT * FROM job where master=%(m)s AND is_active ;', m=master) \
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


def save_job(job: Job, new: bool):
    if new:
        run_query(f'INSERT INTO job (master, title, measurement) VALUES (%(m)s, %(t)s, %(me)s) ;',
                  m=job.master, t=job.title, me=job.measurement)(id)
    else:
        run_query(f"UPDATE job SET is_active = 't' WHERE "
                  f'master = %(m)s AND title = %(t)s AND measurement = %(me)s ;',
                  m=job.master, t=job.title, me=job.measurement)(id)


def save_cjob(completed_job: CompletedJob) -> int:
    def collect(rows):
        for row in rows:
            return int(row[0])
        raise ValueError(f'Expected an int id on returning, but got {str(rows)}')

    return run_query(f'INSERT INTO completed (job_id, count) VALUES (%(ji)s, %(c)s) RETURNING id ;',
                     ji=completed_job.job.id, c=completed_job.count)(collect)


def mark_all_inactive():
    run_query(f"UPDATE job SET is_active = 'f' ;")(id)


def drop_table():
    run_query(f'TRUNCATE TABLE job CASCADE ;')(id)
    run_query(f'TRUNCATE TABLE completed CASCADE ;')(id)
