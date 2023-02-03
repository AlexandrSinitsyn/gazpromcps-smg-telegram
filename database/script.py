import csv
from datetime import datetime

from dto.job import Job
from service.excel_service import ExcelService


def run():
    excel_service = ExcelService()

    excel_service.delete_all()

    with open('./database/full_table.csv', 'r', encoding="utf8") as f:
        data = []

        # section_number, title, measurement, level, ?, idx
        for row in csv.reader(f, delimiter=','):
            if int(row[3]) == 2:
                data.append(Job(-1, float(row[0]), row[1], row[2], datetime.now()))

        excel_service.import_data(data)
