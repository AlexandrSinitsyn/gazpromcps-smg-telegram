from repository.job_repository import *


class JobService:
    @staticmethod
    def get_all() -> List[Job]:
        return find_all()

    @staticmethod
    def get_all_active() -> List[Job]:
        return find_active()

    @staticmethod
    def deactivate_all():
        mark_all_inactive()

    @staticmethod
    def get_by_master(master: str) -> List[Job]:
        return find_by_master(master)

    @staticmethod
    def get_by_stage(stage: str) -> List[Job]:
        return find_by_stage(stage)

    @staticmethod
    def get_by_stage_and_master(stage: str, master: str) -> List[Job]:
        return find_by_stage_and_master(stage, master)

    @staticmethod
    def get_active_by_master(master: str) -> List[Job]:
        return find_active_by_master(master)

    @staticmethod
    def get_active_by_stage(stage: str) -> List[Job]:
        return find_active_by_stage(stage)

    @staticmethod
    def get_by_id(job_id: int) -> Optional[Job]:
        return find_by_id(job_id)

    @staticmethod
    def get_by_params(stage: str, gen_plan: str, master: str, title: str) -> Optional[Job]:
        return find_by_params(stage, gen_plan, master, title)

    @staticmethod
    def day_activity(completed_job: CompletedJob) -> int:
        return save_cjob(completed_job)
