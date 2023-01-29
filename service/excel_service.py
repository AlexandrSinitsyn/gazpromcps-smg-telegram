from repository.job_repository import *


class ExcelService:
    @staticmethod
    def import_data(data: List[Job]):
        for j in data:
            save_job(j)

    @staticmethod
    def import_csv(path: str):
        pass

    @staticmethod
    def delete_all():
        drop_table()

    @staticmethod
    def export_csv() -> str:
        pass
