import csv
from datetime import datetime

from dto.job import Job
from service.excel_service import ExcelService


def run():
    excel_service = ExcelService()

    excel_service.delete_all()

    with open('./database/full_table.csv', 'r', encoding="utf8") as f:
        data = []

        for row in csv.reader(f, delimiter=','):
            for work in row[1].split('|'):
                data.append(Job(-1, row[0], work, datetime.now()))

        excel_service.import_data(data)
