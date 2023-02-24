import csv
import pyexcel

from repository.job_repository import *

path_to_done = '/bot/storage/done/'
path_to_job_list = '/bot/storage/jobs/'

os.makedirs(path_to_done, exist_ok=True)
os.makedirs(path_to_job_list, exist_ok=True)


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
                data.append(Job(int(row[0]), row[1], row[2], row[3],
                                datetime.fromisoformat(row[4])))

        ExcelService.import_data(data)

    @staticmethod
    def rollback(timestamp: datetime):
        ExcelService.import_csv('save_' + str(timestamp) + '.csv')

    @staticmethod
    def delete_all():
        drop_table()

    @staticmethod
    def export_csv() -> str:
        return '\n'.join([CompletedJob.csv_title()] + [str(cj) for cj in collect_daily()])

    @staticmethod
    def export_xlsx() -> List[List[str]]:
        return [CompletedJob.csv_title().replace('"', '').split(',')] +\
               [str(cj).replace('"', '').split(',') for cj in collect_daily()]

    @staticmethod
    def save(xlsx: bool = False) -> str:
        now = datetime.now()

        file_name = path_to_done + 'save_' + datetime.strftime(now, '%Y%m%d_%H%M%S') + ('.xlsx' if xlsx else '.csv')

        if xlsx:
            pyexcel.save_as(array=ExcelService.export_xlsx(), dest_file_name=file_name)
        else:
            with open(file_name, 'w', newline='', encoding='utf-8') as f:
                f.write(ExcelService.export_csv())

        return file_name
