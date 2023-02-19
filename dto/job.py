from dataclasses import dataclass
from datetime import datetime


@dataclass
class Job:
    id: int
    master: str
    title: str
    timestamp: datetime

    @staticmethod
    def fake():
        return Job(-1, 'unknown', 'unknown', datetime.now())

    @staticmethod
    def csv_title() -> str:
        return 'master,title'

    def __str__(self):
        return f'{self.master},{self.title}'


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
