from typing import List, Optional

from repository.database import *
from dto.job import *


def to_job(row) -> Job:
    return Job(row[0], row[1], row[2], row[3], row[4])


def to_cjob(row) -> CompletedJob:
    # row := (id, job_id, count, timestamp)
    job = find_by_id(row[1])

    if job is None:
        job = Job(-1, -1.0, 'unknown or deleted', 'unknown or deleted', datetime.now())

    return CompletedJob(row[0], job, row[2], row[3])


def collect_daily() -> List[CompletedJob]:
    return run_query('SELECT * FROM completed ;') \
        (lambda rows: [to_cjob(row) for row in rows])


def find_all() -> List[Job]:
    return run_query('SELECT * FROM job ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_by_section(section_number: float) -> List[Job]:
    return run_query(f'SELECT * FROM job where section_number=%(sn)s ;', sn=section_number) \
        (lambda rows: [to_job(row) for row in rows])


def find_by_measurement(measurement: str) -> List[Job]:
    return run_query(f'SELECT * FROM job where measurement=%(m)s ;', m=measurement) \
        (lambda rows: [to_job(row) for row in rows])


def find_by_id(job_id: int) -> Optional[Job]:
    def find(rows):
        for row in rows:
            return to_job(row)
        return None

    return run_query('SELECT * FROM job where id=%(ji)s ;', ji=job_id)(find)


def find_by_params(section_number: float, title: str, measurement: str) -> Optional[Job]:
    def find(rows):
        for row in rows:
            return to_job(row)
        return None

    return run_query(
        f'SELECT * FROM job where section_number=%(sn)s AND title=%(t)s AND measurement=%(m)s ;',
        sn=section_number, t=title, m=measurement)(find)


def save_job(job: Job):
    run_query(f'INSERT INTO job (section_number, title, measurement) VALUES (%(sn)s, %(t)s, %(m)s) ;',
              sn=job.section_number, t=job.title, m=job.measurement)(id)


def save_cjob(completed_job: CompletedJob):
    run_query(f'INSERT INTO completed (job_id, count) VALUES (%(ji)s, %(c)s) ;',
              ji=completed_job.job.id, c=completed_job.count)(id)


def drop_table():
    run_query(f'TRUNCATE TABLE job CASCADE ;')(id)
    run_query(f'TRUNCATE TABLE completed CASCADE ;')(id)
