from dataclasses import dataclass
from datetime import datetime


@dataclass
class Job:
    id: int
    section_number: float
    title: str
    measurement: str
    timestamp: datetime

    @staticmethod
    def fake():
        return Job(-1, -1, 'unknown', 'unknown', datetime.now())

    @staticmethod
    def csv_title() -> str:
        return 'section_number,title,measurement'

    def __str__(self):
        return f'{self.section_number},{self.title},{self.measurement}'


@dataclass
class CompletedJob:
    id: int
    job: Job
    count: int
    timestamp: datetime

    @staticmethod
    def csv_title() -> str:
        return Job.csv_title() + ',count,timestamp'

    def __str__(self):
        return str(self.job) + f',{self.count},{self.timestamp}'
