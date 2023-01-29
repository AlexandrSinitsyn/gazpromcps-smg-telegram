import csv

from repository.job_repository import *

path_to_done = '/bot/storage/done/'
path_to_job_list = '/bot/storage/jobs/'


class ExcelService:
    @staticmethod
    def import_data(data: List[Job]):
        for j in data:
            save_job(j)

    @staticmethod
    def import_csv(file_name: str):
        data = []

        with open(path_to_job_list + file_name, 'r') as f:
            r = csv.reader(f, delimiter=',')

            for row in r:
                data.append(Job(int(row[0]), float(row[1]), row[2], row[3],
                                datetime.fromisoformat(row[4])))

        ExcelService.import_data(data)

    @staticmethod
    def rollback(timestamp: datetime):
        ExcelService.import_csv('save_' + str(timestamp))

    @staticmethod
    def delete_all():
        drop_table()

    @staticmethod
    def export_csv() -> str:
        return '\n'.join([CompletedJob.csv_title()] + [str(cj) for cj in collect_daily()])

    @staticmethod
    def save():
        now = datetime.now()

        with open(path_to_done + 'save_' + str(now), 'w', newline='') as f:
            w = csv.writer(f, delimiter=',')

            for row in map(str, collect_daily()):
                w.writerow(row)
