from typing import List

from dto.Job import Job


class JobService:
    def __init__(self):
        pass

    def get_all(self) -> List[Job]:
        pass

    def get_by_section(self, section: str) -> List[Job]:
        pass

    def get_by_measurement(self, measurement: str) -> List[Job]:
        pass

    def get_by_id(self, job_id: int) -> Job:
        pass

    def get_by_params(self, section_number: int, message: str, measurement: str) -> Job:
        pass
