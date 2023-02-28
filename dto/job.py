from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class Job:
    id: int
    master: str
    title: str
    measurement: str
    timestamp: datetime

    @staticmethod
    def fake():
        return Job(-1, 'unknown', 'unknown', 'unknown', datetime.now())

    @staticmethod
    def csv_title() -> str:
        return '"Подрядчик","Вип работ","Единицы измерения"'

    def __str__(self):
        return f'"{self.master}","{self.title}","{self.measurement}"'


@dataclass
class CompletedJob:
    id: int
    job: Job
    count: int
    timestamp: datetime

    @staticmethod
    def csv_title() -> str:
        return Job.csv_title() + ',"Количество","Время отчета"'

    def __str__(self):
        return str(self.job) + f',"{self.count}","{datetime.strftime(self.timestamp,"%d-%m-%Y (%H:%M)")}"'
