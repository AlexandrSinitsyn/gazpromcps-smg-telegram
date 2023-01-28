from typing import List, Optional

from repository.database import *
from dto.job import *


def to_job(row):
    return Job(int(row[0]), float(row[1]), row[2], row[3], datetime.fromtimestamp(int(row[4])))


def find_all() -> List[Job]:
    return run_query('SELECT * FROM job ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_by_section(section_number: int) -> List[Job]:
    return run_query(f'SELECT * FROM job where section_number={section_number} ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_by_measurement(measurement: str) -> List[Job]:
    return run_query(f'SELECT * FROM job where measurement={measurement} ;') \
        (lambda rows: [to_job(row) for row in rows])


def find_by_id(job_id: int) -> Optional[Job]:
    def find(rows):
        for row in rows:
            return to_job(row)
        return None

    return run_query(f'SELECT * FROM job where id={job_id} ;')(find)


def find_by_params(section_number: float, message: str, measurement: str) -> Optional[Job]:
    def find(rows):
        for row in rows:
            return to_job(row)
        return None

    return run_query(
            f'SELECT * FROM job where'
            f'section_number={section_number} AND message={message} AND measurement={measurement} ;')(find)


def save_cjob(completed_job: CompletedJob):
    run_query(f'INSERT INTO completed (job_id, count) VALUES ({completed_job.job.id}, {completed_job.count}) ;')(id)
