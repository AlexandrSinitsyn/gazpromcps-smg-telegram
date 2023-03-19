from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class Job:
    id: int
    stage: str
    master: str
    title: str
    measurement: str
    is_active: bool
    timestamp: datetime

    @staticmethod
    def fake():
        return Job(-1, 'unknown', 'unknown', 'unknown', 'unknown', False, datetime.now())

    @staticmethod
    def csv_title() -> str:
        return '"Этап","Подрядчик","Вип работ","Единицы измерения"'

    def __str__(self):
        return f'"{self.stage}","{self.master}","{self.title}","{self.measurement}"'


@dataclass
class CompletedJob:
    id: int
    job: Job
    count: float
    timestamp: datetime

    @staticmethod
    def csv_title() -> str:
        return Job.csv_title() + ',"Количество","Время отчета"'

    def __str__(self):
        return str(self.job) + f',"{self.count}","{datetime.strftime(self.timestamp,"%d-%m-%Y (%H:%M)")}"'
