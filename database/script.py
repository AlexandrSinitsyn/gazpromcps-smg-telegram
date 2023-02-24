import csv
from datetime import datetime

from dto.job import Job
from service.excel_service import ExcelService


def run():
    excel_service = ExcelService()

    excel_service.delete_all()

    # s/"([^"]*)","([^\|"]+)(\|([^"]+))?"\n/"$1","$2"\n"$1","$4"/gmi
    # s/"[^"]+",""\n//gmi
    with open('./database/full_table.csv', 'r', encoding="utf8") as f:
        data = []

        def capitalize_first(text: str):
            return text[0].upper() + text[1:]

        for row in csv.reader(f, delimiter=','):
            # row := "title","master","measurement"
            data.append(Job(-1, row[1], capitalize_first(row[0]), row[2], datetime.now()))

        excel_service.import_data(data)
