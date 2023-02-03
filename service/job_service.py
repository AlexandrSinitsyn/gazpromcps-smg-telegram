from repository.job_repository import *


class JobService:
    @staticmethod
    def get_all() -> List[Job]:
        return find_all()

    @staticmethod
    def get_by_section(section_number: float) -> List[Job]:
        return find_by_section(section_number)

    @staticmethod
    def get_by_measurement(measurement: str) -> List[Job]:
        return find_by_measurement(measurement)

    @staticmethod
    def get_by_id(job_id: int) -> Optional[Job]:
        return find_by_id(job_id)

    @staticmethod
    def get_by_params(section_number: float, title: str, measurement: str) -> Optional[Job]:
        return find_by_params(section_number, title, measurement)

    @staticmethod
    def day_activity(completed_job: CompletedJob):
        save_cjob(completed_job)
