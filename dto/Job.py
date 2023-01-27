from dataclasses import dataclass


@dataclass
class Job:
    id: int
    section_number: int
    message: str
    measurement: str
    count: int
