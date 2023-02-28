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
    def summarise(start: datetime) -> List[str]:
        total = collect_daily(start)

        res = {}
        for cj in total:
            if cj.job not in res:
                res[cj.job] = 0
            res[cj.job] += cj.count

        return [str(CompletedJob(-1, k, v, datetime.now()))for k, v in res.items()]

    @staticmethod
    def export_csv(start: datetime) -> str:
        return '\n'.join([CompletedJob.csv_title()] + [str(cj) for cj in collect_daily(start)] +\
                         [] + ['Сумма по каждому виду работ'] + ExcelService.summarise(start))

    @staticmethod
    def export_xlsx(start: datetime) -> List[List[str]]:
        return [[e.replace('"', '') for e in CompletedJob.csv_title().split('","')]] +\
               [[e.replace('"', '') for e in str(cj).split('","')] for cj in collect_daily(start)] +\
               [[]] + [['Сумма по каждому виду работ']] +\
               [[e.replace('"', '') for e in str(cj).split('","')] for cj in ExcelService.summarise(start)]

    @staticmethod
    def save(start: datetime, xlsx: bool = False) -> str:
        now = datetime.now()

        file_name = path_to_done + 'save_' + datetime.strftime(now, '%Y%m%d_%H%M%S') + ('.xlsx' if xlsx else '.csv')

        if xlsx:
            pyexcel.save_as(array=ExcelService.export_xlsx(start), dest_file_name=file_name)
        else:
            with open(file_name, 'w', newline='', encoding='utf-8') as f:
                f.write(ExcelService.export_csv(start))

        return file_name
