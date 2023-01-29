from dataclasses import dataclass
from datetime import datetime


@dataclass
class Job:
    id: int
    section_number: float
    title: str
    measurement: str
    timestamp: datetime


@dataclass
class CompletedJob:
    id: int
    job: Job
    count: int
    timestamp: datetime
